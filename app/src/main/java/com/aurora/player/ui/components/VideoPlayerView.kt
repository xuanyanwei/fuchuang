package com.aurora.player.ui.components

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aurora.player.player.AspectRatioMode
import com.aurora.player.player.AuroraPlayerController
import com.aurora.player.player.PlayerState
import com.aurora.player.ui.theme.AuroraCyan

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    controller: AuroraPlayerController,
    playerState: PlayerState,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onDoubleTapLeft: () -> Unit = {},
    onDoubleTapRight: () -> Unit = {}
) {
    LocalContext.current

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x < width * 0.35f) {
                            onDoubleTapLeft()
                        } else if (offset.x > width * 0.65f) {
                            onDoubleTapRight()
                        } else {
                            controller.togglePlayPause()
                        }
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    player = controller.exoPlayer
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            update = { playerView ->
                playerView.player = controller.exoPlayer
                playerView.resizeMode = when (playerState.aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FIXED_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (playerState.isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AuroraCyan,
                    strokeWidth = 3.dp
                )
            }
        }

        DisposableEffect(controller) {
            onDispose {
                // Keep player reference clean
            }
        }
    }
}
