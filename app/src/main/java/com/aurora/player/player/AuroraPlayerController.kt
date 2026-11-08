package com.aurora.player.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.aurora.player.data.model.PlaylistItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

enum class AspectRatioMode(val displayName: String) {
    FIT("适应窗口"),
    FILL("全屏填充"),
    FIXED_16_9("16:9 比例"),
    FIXED_4_3("4:3 比例")
}

data class PlayerState(
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isLive: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val currentItem: PlaylistItem? = null,
    val errorMessage: String? = null,
    val isControlsLocked: Boolean = false,
    val isMuted: Boolean = false,
    val volume: Float = 1.0f,
    val isBuffering: Boolean = false
) {
    val resolutionText: String
        get() = if (videoWidth > 0 && videoHeight > 0) "${videoWidth}x${videoHeight}" else "解析中..."
}

class AuroraPlayerController(
    private val context: Context,
    private val onPositionSaved: (PlaylistItem, Long, Long) -> Unit = { _, _, _ -> }
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackingJob: Job? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    var exoPlayer: ExoPlayer? = null
        private set

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        exoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build()
            .apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        val isBuffering = state == Player.STATE_BUFFERING
                        val dur = if (duration != C.TIME_UNSET && duration > 0) duration else 0L
                        val isLive = isCurrentMediaItemLive || duration == C.TIME_UNSET

                        _playerState.value = _playerState.value.copy(
                            playbackState = state,
                            isBuffering = isBuffering,
                            duration = dur,
                            isLive = isLive
                        )

                        if (state == Player.STATE_READY) {
                            startProgressTracking()
                        } else if (state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                            saveCurrentPosition()
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                        if (isPlaying) {
                            startProgressTracking()
                        } else {
                            saveCurrentPosition()
                        }
                    }

                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        _playerState.value = _playerState.value.copy(
                            videoWidth = videoSize.width,
                            videoHeight = videoSize.height
                        )
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _playerState.value = _playerState.value.copy(
                            errorMessage = error.localizedMessage ?: "播放异常，请检查网络或流地址",
                            isBuffering = false
                        )
                    }
                })
            }
    }

    fun play(item: PlaylistItem, startPositionMs: Long = 0L, customHeaders: Map<String, String>? = null) {
        val player = exoPlayer ?: return
        val headers = customHeaders ?: item.headers

        // Configure custom headers for OkHttp data source
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .apply {
                if (headers.isNotEmpty()) {
                    setDefaultRequestProperties(headers)
                }
            }

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        val mediaSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(item.url))

        _playerState.value = _playerState.value.copy(
            currentItem = item,
            errorMessage = null,
            currentPosition = startPositionMs,
            duration = 0L,
            videoWidth = 0,
            videoHeight = 0
        )

        player.setMediaSource(mediaSource)
        player.prepare()
        if (startPositionMs > 0) {
            player.seekTo(startPositionMs)
        }
        player.playWhenReady = true
        startProgressTracking()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPosition = positionMs)
    }

    fun seekForward(deltaMs: Long = 10000L) {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition + deltaMs).coerceAtMost(player.duration.coerceAtLeast(0L))
        player.seekTo(newPos)
    }

    fun seekBackward(deltaMs: Long = 10000L) {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition - deltaMs).coerceAtLeast(0L)
        player.seekTo(newPos)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
    }

    fun setAspectRatioMode(mode: AspectRatioMode) {
        _playerState.value = _playerState.value.copy(aspectRatioMode = mode)
    }

    fun toggleControlsLock() {
        _playerState.value = _playerState.value.copy(isControlsLocked = !_playerState.value.isControlsLocked)
    }

    fun toggleMute() {
        val player = exoPlayer ?: return
        val newMuted = !_playerState.value.isMuted
        if (newMuted) {
            player.volume = 0f
        } else {
            player.volume = _playerState.value.volume
        }
        _playerState.value = _playerState.value.copy(isMuted = newMuted)
    }

    fun setVolume(volume: Float) {
        val player = exoPlayer ?: return
        val clamped = volume.coerceIn(0f, 1f)
        player.volume = clamped
        _playerState.value = _playerState.value.copy(volume = clamped, isMuted = clamped == 0f)
    }

    fun playNextEpisode(items: List<PlaylistItem>): Boolean {
        val current = _playerState.value.currentItem ?: return false
        val index = items.indexOfFirst { it.url == current.url }
        if (index != -1 && index + 1 < items.size) {
            play(items[index + 1])
            return true
        }
        return false
    }

    fun playPreviousEpisode(items: List<PlaylistItem>): Boolean {
        val current = _playerState.value.currentItem ?: return false
        val index = items.indexOfFirst { it.url == current.url }
        if (index > 0) {
            play(items[index - 1])
            return true
        }
        return false
    }

    private fun startProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch {
            while (isActive) {
                val player = exoPlayer
                if (player != null && player.playbackState == Player.STATE_READY) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = if (player.duration != C.TIME_UNSET) player.duration else 0L
                    _playerState.value = _playerState.value.copy(
                        currentPosition = pos,
                        duration = dur
                    )
                }
                delay(500)
            }
        }
    }

    fun saveCurrentPosition() {
        val item = _playerState.value.currentItem ?: return
        val pos = exoPlayer?.currentPosition ?: 0L
        val dur = exoPlayer?.duration ?: 0L
        if (pos > 1000L) {
            onPositionSaved(item, pos, if (dur > 0) dur else 0L)
        }
    }

    fun release() {
        saveCurrentPosition()
        progressTrackingJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
