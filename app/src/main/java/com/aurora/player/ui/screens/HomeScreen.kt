package com.aurora.player.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.player.data.model.PlaylistGroup
import com.aurora.player.data.model.PlaylistItem
import com.aurora.player.player.AuroraPlayerController
import com.aurora.player.player.PlayerState
import com.aurora.player.ui.components.EpisodeSelectorSheet
import com.aurora.player.ui.components.HeaderConfigDialog
import com.aurora.player.ui.components.PlayerControlsOverlay
import com.aurora.player.ui.components.VideoPlayerView
import com.aurora.player.ui.theme.AuroraBorder
import com.aurora.player.ui.theme.AuroraCyan
import com.aurora.player.ui.theme.AuroraDarkBackground
import com.aurora.player.ui.theme.AuroraDarkSurface
import com.aurora.player.ui.theme.AuroraDarkSurfaceVariant
import com.aurora.player.ui.theme.AuroraNeonBlue
import com.aurora.player.ui.theme.AuroraPurple
import com.aurora.player.ui.theme.AuroraTextPrimary
import com.aurora.player.ui.theme.AuroraTextSecondary

@Composable
fun HomeScreen(
    controller: AuroraPlayerController,
    playerState: PlayerState,
    groupedPlaylist: List<PlaylistGroup>,
    allPlaylistItems: List<PlaylistItem>,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onEnterPip: () -> Unit,
    onPlayItem: (PlaylistItem) -> Unit,
    onAddSingleStream: (String, String) -> Unit,
    onOpenPlaylistTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputUrl by remember { mutableStateOf("") }
    var inputTitle by remember { mutableStateOf("") }
    var showOverlay by remember { mutableStateOf(true) }
    var showEpisodesSheet by remember { mutableStateOf(false) }

    // If fullscreen, render only player view and overlay
    if (isFullscreen) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                playlist = allPlaylistItems,
                onToggleFullscreen = onToggleFullscreen,
                onEnterPip = onEnterPip,
                onOpenEpisodesSheet = { showEpisodesSheet = true },
                onBack = onToggleFullscreen
            )

            if (showEpisodesSheet) {
                EpisodeSelectorSheet(
                    groups = groupedPlaylist,
                    currentPlayingItem = playerState.currentItem,
                    onSelectEpisode = { item ->
                        onPlayItem(item)
                        showEpisodesSheet = false
                    },
                    onDismiss = { showEpisodesSheet = false }
                )
            }
        }
        return
    }

    // Normal Portrait Layout
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuroraDarkBackground)
    ) {
        // Player Container (16:9)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
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
                playlist = allPlaylistItems,
                onToggleFullscreen = onToggleFullscreen,
                onEnterPip = onEnterPip,
                onOpenEpisodesSheet = { showEpisodesSheet = true },
                onBack = {}
            )
        }

        // Scrollable content beneath player
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stream Quick Input Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuroraDarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "快速播放流地址 (M3U8 / MP4 / 直播流)",
                                color = AuroraTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!clip.isNullOrBlank()) {
                                        inputUrl = clip.trim()
                                    }
                                },
                                modifier = Modifier.size(32.dp).testTag("paste_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "粘贴剪贴板",
                                    tint = AuroraCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            placeholder = { Text("输入或粘贴流媒体 URL (如 http://.../live.m3u8)", fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuroraCyan,
                                unfocusedBorderColor = AuroraBorder,
                                focusedTextColor = AuroraTextPrimary,
                                unfocusedTextColor = AuroraTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("stream_url_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputTitle,
                                onValueChange = { inputTitle = it },
                                placeholder = { Text("流名称 (选填)", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AuroraCyan,
                                    unfocusedBorderColor = AuroraBorder,
                                    focusedTextColor = AuroraTextPrimary,
                                    unfocusedTextColor = AuroraTextPrimary
                                ),
                                modifier = Modifier.weight(1f).testTag("stream_title_input")
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = {
                                    if (inputUrl.isNotBlank()) {
                                        onAddSingleStream(inputTitle, inputUrl.trim())
                                        inputUrl = ""
                                        inputTitle = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp).testTag("play_input_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = AuroraDarkBackground
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("立即播放", color = AuroraDarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Current Group Episodes Quick Row (分集选集)
            val currentItem = playerState.currentItem
            val currentGroup = groupedPlaylist.firstOrNull { it.name == currentItem?.groupTitle }
            if (currentGroup != null && currentGroup.items.size > 1) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "剧集选集 • ${currentGroup.name}",
                                color = AuroraTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "共 ${currentGroup.items.size} 集 >",
                                color = AuroraCyan,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { showEpisodesSheet = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentGroup.items) { item ->
                                val isSelected = item.url == currentItem?.url
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AuroraCyan.copy(alpha = 0.2f) else AuroraDarkSurface,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AuroraCyan) else null,
                                    modifier = Modifier
                                        .clickable { onPlayItem(item) }
                                        .testTag("quick_ep_${item.episodeNumber ?: item.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.PlayCircle,
                                                contentDescription = null,
                                                tint = AuroraCyan,
                                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = item.episodeNumber?.let { "第${it}集" } ?: item.title.take(8),
                                            color = if (isSelected) AuroraCyan else AuroraTextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Official Demo Streams Row
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "精选与官方验证测试流",
                            color = AuroraTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "查看全部播放列表",
                            color = AuroraNeonBlue,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onOpenPlaylistTab() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val demos = allPlaylistItems.take(5)
                        items(demos) { demo ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AuroraDarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .width(200.dp)
                                    .clickable { onPlayItem(demo) }
                                    .testTag("demo_card_${demo.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AuroraPurple.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = demo.groupTitle,
                                            color = AuroraPurple,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = demo.title,
                                        color = AuroraTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "点击即刻播放",
                                        color = AuroraCyan,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stream Diagnostics & Player Information
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuroraDarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "流媒体运行诊断",
                            color = AuroraCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "分辨率: ${playerState.resolutionText}", color = AuroraTextSecondary, fontSize = 12.sp)
                            Text(text = "流类型: ${if (playerState.isLive) "直播流 (LIVE)" else "点播流 (VOD)"}", color = AuroraTextSecondary, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "播放速度: ${playerState.playbackSpeed}x", color = AuroraTextSecondary, fontSize = 12.sp)
                            Text(text = "画面适应: ${playerState.aspectRatioMode.displayName}", color = AuroraTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showEpisodesSheet) {
        EpisodeSelectorSheet(
            groups = groupedPlaylist,
            currentPlayingItem = playerState.currentItem,
            onSelectEpisode = { item ->
                onPlayItem(item)
                showEpisodesSheet = false
            },
            onDismiss = { showEpisodesSheet = false }
        )
    }
}
