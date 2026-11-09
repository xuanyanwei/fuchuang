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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
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
import com.aurora.player.player.AuroraPlayerController
import com.aurora.player.player.PlayerState
import com.aurora.player.ui.theme.AuroraBlack
import com.aurora.player.ui.theme.AuroraOrange
import com.aurora.player.ui.theme.AuroraTextPrimary
import com.aurora.player.ui.theme.AuroraTextSecondary
import kotlinx.coroutines.delay

@Composable
fun PlayerControlsOverlay(
    controller: AuroraPlayerController,
    playerState: PlayerState,
    isVisible: Boolean,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenSettings: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    // Auto hide after 4.5 seconds if playing
    LaunchedEffect(isVisible, playerState.isPlaying, isSeeking) {
        if (isVisible && playerState.isPlaying && !isSeeking) {
            delay(4500)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Center overlay message when waiting or buffering
        if (playerState.currentItem == null || playerState.isBuffering || !playerState.isPlaying && playerState.currentPosition == 0L) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xFF14161F).copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = AuroraOrange,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (playerState.currentItem == null) {
                            "请在下方输入流媒体链接并点击播放"
                        } else if (playerState.isBuffering) {
                            "正在缓冲流媒体..."
                        } else {
                            "点击播放继续观看"
                        },
                        color = AuroraTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Visible controls overlay
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tag "极光" + Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AuroraOrange,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "极光",
                                color = AuroraBlack,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = playerState.currentItem?.title?.ifBlank { "等待播放源" } ?: "等待播放源",
                            color = AuroraTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Three dots menu button (opens settings)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onOpenSettings
                            )
                            .testTag("three_dots_settings_button")
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "播放器设置",
                                tint = AuroraTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Bottom Controls Bar (Seekbar + Buttons Row)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Top: Seekbar
                    val currentPos = if (isSeeking) seekPosition else playerState.currentPosition.toFloat()
                    val totalDuration = playerState.duration.toFloat().coerceAtLeast(1f)

                    Slider(
                        value = currentPos.coerceIn(0f, totalDuration),
                        onValueChange = {
                            isSeeking = true
                            seekPosition = it
                        },
                        onValueChangeFinished = {
                            controller.seekTo(seekPosition.toLong())
                            isSeeking = false
                        },
                        valueRange = 0f..totalDuration,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .testTag("player_seekbar")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom Row: Play, Prev, Next, Time, Volume, Pip, Fullscreen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Play / Pause
                            IconButton(
                                onClick = { controller.togglePlayPause() },
                                modifier = Modifier.size(36.dp).testTag("play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Previous
                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier.size(36.dp).testTag("prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "上一集",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Next
                            IconButton(
                                onClick = onNext,
                                modifier = Modifier.size(36.dp).testTag("next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "下一集",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Time: 00:00 / 00:00
                            val currentSec = (if (isSeeking) (seekPosition / 1000).toLong() else (playerState.currentPosition / 1000)).coerceAtLeast(0)
                            val totalSec = (playerState.duration / 1000).coerceAtLeast(0)

                            Text(
                                text = "${formatSeconds(currentSec)} / ${if (playerState.isLive) "LIVE" else formatSeconds(totalSec)}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Right controls: Volume, PiP, Fullscreen
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Volume Icon
                            IconButton(
                                onClick = { controller.toggleMute() },
                                modifier = Modifier.size(34.dp).testTag("volume_toggle")
                            ) {
                                Icon(
                                    imageVector = if (playerState.isMuted || playerState.volume == 0f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "音量",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Volume Slider (orange active track and orange thumb as shown in screenshot)
                            Slider(
                                value = if (playerState.isMuted) 0f else playerState.volume,
                                onValueChange = { newVol ->
                                    controller.setVolume(newVol)
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AuroraOrange,
                                    activeTrackColor = AuroraOrange,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(20.dp)
                                    .testTag("volume_slider")
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // PiP button
                            IconButton(
                                onClick = onEnterPip,
                                modifier = Modifier.size(34.dp).testTag("pip_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "画中画",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Fullscreen toggle button
                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier.size(34.dp).testTag("fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                                    tint = AuroraOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSeconds(seconds: Long): String {
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
