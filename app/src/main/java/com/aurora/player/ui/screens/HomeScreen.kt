package com.aurora.player.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.player.data.model.PlaylistItem
import com.aurora.player.data.model.StreamHeaderConfig
import com.aurora.player.player.AspectRatioMode
import com.aurora.player.player.AuroraPlayerController
import com.aurora.player.player.PlayerState
import com.aurora.player.ui.components.HeaderConfigDialog
import com.aurora.player.ui.components.PlayerControlsOverlay
import com.aurora.player.ui.components.PlayerSettingsConfig
import com.aurora.player.ui.components.PlayerSettingsSheet
import com.aurora.player.ui.components.VideoPlayerView
import com.aurora.player.ui.theme.AuroraBlack
import com.aurora.player.ui.theme.AuroraBorder
import com.aurora.player.ui.theme.AuroraBtnGrey
import com.aurora.player.ui.theme.AuroraBtnGreyText
import com.aurora.player.ui.theme.AuroraDarkBackground
import com.aurora.player.ui.theme.AuroraDarkSurface
import com.aurora.player.ui.theme.AuroraOrange
import com.aurora.player.ui.theme.AuroraTextPrimary
import com.aurora.player.ui.theme.AuroraTextSecondary

@Composable
fun HomeScreen(
    controller: AuroraPlayerController,
    playerState: PlayerState,
    headerConfig: StreamHeaderConfig,
    onSaveHeaderConfig: (StreamHeaderConfig) -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onEnterPip: () -> Unit,
    onPlayUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputUrl by remember { mutableStateOf("") }
    var showOverlay by remember { mutableStateOf(true) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showHeadersDialog by remember { mutableStateOf(false) }

    var settingsConfig by remember {
        mutableStateOf(
            PlayerSettingsConfig(
                forceFillScreen = false,
                coverStretch = false,
                autoOrientation = true,
                loopPlayback = false,
                randomPlay = false,
                autoNextEpisode = true
            )
        )
    }

    var isRelayProxyEnabled by remember { mutableStateOf(false) }

    // If in fullscreen mode, fill the whole display with player view and controls
    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayerView(
                controller = controller,
                playerState = playerState,
                onTap = { showOverlay = !showOverlay },
                onDoubleTapLeft = { controller.seekBackward(10000) },
                onDoubleTapRight = { controller.seekForward(10000) },
                modifier = Modifier.fillMaxSize()
            )

            PlayerControlsOverlay(
                controller = controller,
                playerState = playerState,
                isVisible = showOverlay,
                isFullscreen = true,
                onToggleFullscreen = onToggleFullscreen,
                onEnterPip = onEnterPip,
                onOpenSettings = { showSettingsSheet = true },
                onPrevious = { controller.seekBackward(10000) },
                onNext = { controller.seekForward(10000) }
            )

            if (showSettingsSheet) {
                PlayerSettingsSheet(
                    playerState = playerState,
                    settingsConfig = settingsConfig,
                    onUpdateConfig = { settingsConfig = it },
                    onSetPlaybackSpeed = { controller.setPlaybackSpeed(it) },
                    onSetAspectRatio = { controller.setAspectRatioMode(it) },
                    onDismiss = { showSettingsSheet = false }
                )
            }
        }
        return
    }

    // Normal Portrait View
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuroraDarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // --- 1. Top Status Banner ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "当前流媒体: ",
                color = AuroraTextSecondary,
                fontSize = 13.sp
            )
            if (playerState.currentItem == null) {
                Text(
                    text = "未加载",
                    color = AuroraOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            } else {
                Text(
                    text = "已就绪",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Text(
                text = " • 请在下方输入播放地址",
                color = AuroraTextSecondary,
                fontSize = 13.sp
            )
        }

        // --- 2. Top Action Chips (跨域中继代理 & 请求头配置) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Chip 1: 跨域中继代理
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AuroraDarkSurface,
                border = BorderStroke(1.dp, AuroraBorder),
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        isRelayProxyEnabled = !isRelayProxyEnabled
                        Toast.makeText(
                            context,
                            if (isRelayProxyEnabled) "已启用请求头中继模式" else "当前模式: 原生直连",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .testTag("relay_proxy_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AuroraTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRelayProxyEnabled) "跨域中继: 代理" else "跨域中继代理: 直连",
                        color = AuroraTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Chip 2: 请求头配置
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AuroraDarkSurface,
                border = BorderStroke(1.dp, AuroraBorder),
                modifier = Modifier
                    .weight(1f)
                    .clickable { showHeadersDialog = true }
                    .testTag("header_config_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = AuroraTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "请求头配置",
                        color = AuroraTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // --- 3. Video Player Container ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            VideoPlayerView(
                controller = controller,
                playerState = playerState,
                onTap = { showOverlay = !showOverlay },
                onDoubleTapLeft = { controller.seekBackward(10000) },
                onDoubleTapRight = { controller.seekForward(10000) },
                modifier = Modifier.fillMaxSize()
            )

            PlayerControlsOverlay(
                controller = controller,
                playerState = playerState,
                isVisible = showOverlay,
                isFullscreen = false,
                onToggleFullscreen = onToggleFullscreen,
                onEnterPip = onEnterPip,
                onOpenSettings = { showSettingsSheet = true },
                onPrevious = { controller.seekBackward(10000) },
                onNext = { controller.seekForward(10000) }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 4. Card: 更换流媒体播放链接 ---
        Card(
            colors = CardDefaults.cardColors(containerColor = AuroraDarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "更换流媒体播放链接",
                    color = AuroraTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "支持 M3U8、MP4、FLV 或多集 M3U 播放列表链接:",
                    color = AuroraTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Input Field
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    placeholder = {
                        Text(
                            text = "https://... (粘贴或输入流媒体视频链接)",
                            color = Color(0xFF5E6577),
                            fontSize = 13.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuroraOrange,
                        unfocusedBorderColor = Color(0xFF282B37),
                        focusedContainerColor = Color(0xFF0F1017),
                        unfocusedContainerColor = Color(0xFF0F1017),
                        focusedTextColor = AuroraTextPrimary,
                        unfocusedTextColor = AuroraTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_url_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons: 清空 & 立即播放
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 清空 Button
                    Button(
                        onClick = { inputUrl = "" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuroraBtnGrey,
                            contentColor = AuroraBtnGreyText
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .padding(end = 10.dp)
                            .testTag("clear_url_button")
                    ) {
                        Text(
                            text = "清空",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 立即播放 Button
                    Button(
                        onClick = {
                            if (inputUrl.isNotBlank()) {
                                onPlayUrl(inputUrl.trim())
                            } else {
                                Toast.makeText(context, "请输入有效的流媒体播放地址", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuroraOrange,
                            contentColor = AuroraBlack
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("play_stream_button")
                    ) {
                        Text(
                            text = "立即播放",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Settings Bottom Sheet (triggered by 3-dots icon)
    if (showSettingsSheet) {
        PlayerSettingsSheet(
            playerState = playerState,
            settingsConfig = settingsConfig,
            onUpdateConfig = { settingsConfig = it },
            onSetPlaybackSpeed = { controller.setPlaybackSpeed(it) },
            onSetAspectRatio = { controller.setAspectRatioMode(it) },
            onDismiss = { showSettingsSheet = false }
        )
    }

    // Headers Configuration Dialog
    if (showHeadersDialog) {
        HeaderConfigDialog(
            initialConfig = headerConfig,
            onSave = { newConfig ->
                onSaveHeaderConfig(newConfig)
                showHeadersDialog = false
            },
            onDismiss = { showHeadersDialog = false }
        )
    }
}
