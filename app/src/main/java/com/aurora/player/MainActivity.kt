package com.aurora.player

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aurora.player.data.model.PlaylistItem
import com.aurora.player.data.repository.PlaylistRepository
import com.aurora.player.player.AuroraPlayerController
import com.aurora.player.ui.screens.HomeScreen
import com.aurora.player.ui.theme.AuroraDarkBackground
import com.aurora.player.ui.theme.AuroraPlayerTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var repository: PlaylistRepository
    private lateinit var playerController: AuroraPlayerController

    private var isFullscreenMode by mutableStateOf(false)
    private var isInPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on during video playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Support display cutout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        repository = PlaylistRepository(this)
        playerController = AuroraPlayerController(
            context = this,
            onPositionSaved = { item, pos, dur ->
                repository.recordHistory(item, pos, dur)
            }
        )

        setContent {
            AuroraPlayerTheme {
                MainContent()
            }
        }
    }

    @Composable
    private fun MainContent() {
        val playerState by playerController.playerState.collectAsState()
        val headerConfig by repository.headerConfig.collectAsState()

        // Back Handler for exiting fullscreen
        BackHandler(enabled = isFullscreenMode) {
            setFullscreen(false)
        }

        Scaffold(
            containerColor = AuroraDarkBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullscreenMode || isInPipMode) androidx.compose.foundation.layout.PaddingValues() else paddingValues)
            ) {
                HomeScreen(
                    controller = playerController,
                    playerState = playerState,
                    headerConfig = headerConfig,
                    onSaveHeaderConfig = { newConfig ->
                        repository.updateHeaderConfig(newConfig)
                    },
                    isFullscreen = isFullscreenMode,
                    onToggleFullscreen = { toggleFullscreen() },
                    onEnterPip = { enterPip() },
                    onPlayUrl = { url ->
                        val title = try {
                            val path = url.substringBefore('?').substringBefore('#')
                            val fname = path.substringAfterLast('/')
                            if (fname.isNotBlank()) fname.substringBeforeLast('.') else "在线流媒体"
                        } catch (_: Exception) {
                            "在线流媒体"
                        }
                        val item = PlaylistItem(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            url = url,
                            headers = headerConfig.toHeaderMap()
                        )
                        playerController.play(item)
                    }
                )
            }
        }
    }

    private fun setFullscreen(enable: Boolean) {
        isFullscreenMode = enable
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (enable) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun toggleFullscreen() {
        setFullscreen(!isFullscreenMode)
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    override fun onStop() {
        super.onStop()
        playerController.saveCurrentPosition()
        if (!isInPipMode) {
            playerController.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerController.release()
    }
}
