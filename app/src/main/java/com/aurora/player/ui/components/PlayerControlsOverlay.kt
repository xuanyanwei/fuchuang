package com.aurora.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.player.data.model.PlaylistItem
import com.aurora.player.player.AspectRatioMode
import com.aurora.player.player.AuroraPlayerController
import com.aurora.player.player.PlayerState
import com.aurora.player.ui.theme.AuroraCyan
import com.aurora.player.ui.theme.AuroraDarkBackground
import com.aurora.player.ui.theme.AuroraDarkSurface
import com.aurora.player.ui.theme.AuroraError
import com.aurora.player.ui.theme.AuroraNeonBlue
import com.aurora.player.ui.theme.AuroraTextPrimary
import com.aurora.player.ui.theme.AuroraTextSecondary
import kotlinx.coroutines.delay

@Composable
fun PlayerControlsOverlay(
    controller: AuroraPlayerController,
    playerState: PlayerState,
    isVisible: Boolean,
    isFullscreen: Boolean,
    playlist: List<PlaylistItem>,
    onToggleFullscreen: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenEpisodesSheet: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showAspectMenu by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    // Auto hide after 5 seconds if playing
    LaunchedEffect(isVisible, playerState.isPlaying, isSeeking) {
        if (isVisible && playerState.isPlaying && !isSeeking && !playerState.isControlsLocked) {
            delay(4500)
            // Can trigger hide through parent if needed
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            // Locked mode overlay
            if (playerState.isControlsLocked) {
                IconButton(
                    onClick = { controller.toggleControlsLock() },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                        .background(AuroraDarkSurface.copy(alpha = 0.85f), CircleShape)
                        .testTag("unlock_controls_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "解锁控制面板",
                        tint = AuroraCyan
                    )
                }
                return@Box
            }

            // Top gradient & bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = AuroraTextPrimary
                            )
                        }

                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = playerState.currentItem?.title ?: "极光流媒体播放",
                                color = AuroraTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (playerState.isLive) {
                                    Surface(
                                        color = AuroraError,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = "LIVE 直播",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${playerState.currentItem?.groupTitle ?: "默认"} • ${playerState.resolutionText}",
                                    color = AuroraTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Top Action buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Lock Controls button
                        IconButton(
                            onClick = { controller.toggleControlsLock() },
                            modifier = Modifier.testTag("lock_controls_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "锁定屏幕",
                                tint = AuroraTextSecondary
                            )
                        }

                        // Aspect Ratio button
                        Box {
                            IconButton(
                                onClick = { showAspectMenu = true },
                                modifier = Modifier.testTag("aspect_ratio_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitScreen,
                                    contentDescription = "画面比例",
                                    tint = AuroraCyan
                                )
                            }
                            DropdownMenu(
                                expanded = showAspectMenu,
                                onDismissRequest = { showAspectMenu = false }
                            ) {
                                AspectRatioMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = mode.displayName,
                                                color = if (playerState.aspectRatioMode == mode) AuroraCyan else AuroraTextPrimary
                                            )
                                        },
                                        onClick = {
                                            controller.setAspectRatioMode(mode)
                                            showAspectMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Speed selector button
                        Box {
                            IconButton(
                                onClick = { showSpeedMenu = true },
                                modifier = Modifier.testTag("playback_speed_button")
                            ) {
                                Text(
                                    text = "${playerState.playbackSpeed}x",
                                    color = AuroraCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${speed}x",
                                                color = if (playerState.playbackSpeed == speed) AuroraCyan else AuroraTextPrimary
                                            )
                                        },
                                        onClick = {
                                            controller.setPlaybackSpeed(speed)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Episodes selector
                        IconButton(
                            onClick = onOpenEpisodesSheet,
                            modifier = Modifier.testTag("episodes_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewList,
                                contentDescription = "分集选集",
                                tint = AuroraTextPrimary
                            )
                        }

                        // PiP button
                        IconButton(
                            onClick = onEnterPip,
                            modifier = Modifier.testTag("pip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "画中画",
                                tint = AuroraTextPrimary
                            )
                        }
                    }
                }
            }

            // Center playback controls
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Episode
                IconButton(
                    onClick = { controller.playPreviousEpisode(playlist) },
                    modifier = Modifier.testTag("prev_episode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "上一集",
                        tint = AuroraTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Rewind 10s
                IconButton(
                    onClick = { controller.seekBackward(10000) },
                    modifier = Modifier.testTag("rewind_10s_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "快退10秒",
                        tint = AuroraTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Play / Pause central button
                Surface(
                    shape = CircleShape,
                    color = AuroraCyan,
                    modifier = Modifier
                        .size(64.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { controller.togglePlayPause() }
                        )
                        .testTag("play_pause_button"),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                            tint = AuroraDarkBackground,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Forward 10s
                IconButton(
                    onClick = { controller.seekForward(10000) },
                    modifier = Modifier.testTag("forward_10s_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "快进10秒",
                        tint = AuroraTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Next Episode
                IconButton(
                    onClick = { controller.playNextEpisode(playlist) },
                    modifier = Modifier.testTag("next_episode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "下一集",
                        tint = AuroraTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Bottom gradient & seek bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    // Time and Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentSec = if (isSeeking) (seekPosition / 1000).toLong() else (playerState.currentPosition / 1000)
                        val totalSec = (playerState.duration / 1000).coerceAtLeast(0)

                        Text(
                            text = formatTime(currentSec),
                            color = AuroraTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Seek bar (only for non-live streams)
                        if (!playerState.isLive && playerState.duration > 0) {
                            Slider(
                                value = if (isSeeking) seekPosition else playerState.currentPosition.toFloat(),
                                onValueChange = {
                                    isSeeking = true
                                    seekPosition = it
                                },
                                onValueChangeFinished = {
                                    controller.seekTo(seekPosition.toLong())
                                    isSeeking = false
                                },
                                valueRange = 0f..playerState.duration.toFloat().coerceAtLeast(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = AuroraCyan,
                                    activeTrackColor = AuroraCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .testTag("player_seek_slider")
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "实时广播",
                                color = AuroraCyan,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        Text(
                            text = if (playerState.isLive) "LIVE" else formatTime(totalSec),
                            color = AuroraTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Fullscreen toggle
                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.size(36.dp).testTag("fullscreen_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isFullscreen) "退出全屏" else "全屏播放",
                                tint = AuroraCyan
                            )
                        }
                    }
                }
            }

            // Error state banner
            if (playerState.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "播放遇到问题",
                            color = AuroraError,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = playerState.errorMessage,
                            color = AuroraTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                playerState.currentItem?.let { controller.play(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan),
                            modifier = Modifier.testTag("retry_play_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = AuroraDarkBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("重试连接", color = AuroraDarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val sec = seconds.coerceAtLeast(0)
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}
