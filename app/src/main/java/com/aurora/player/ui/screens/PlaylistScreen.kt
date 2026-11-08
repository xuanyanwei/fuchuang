package com.aurora.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.player.data.model.PlaylistGroup
import com.aurora.player.data.model.PlaylistItem
import com.aurora.player.ui.theme.AuroraBorder
import com.aurora.player.ui.theme.AuroraCyan
import com.aurora.player.ui.theme.AuroraDarkBackground
import com.aurora.player.ui.theme.AuroraDarkSurface
import com.aurora.player.ui.theme.AuroraDarkSurfaceVariant
import com.aurora.player.ui.theme.AuroraPink
import com.aurora.player.ui.theme.AuroraTextPrimary
import com.aurora.player.ui.theme.AuroraTextSecondary
import kotlinx.coroutines.launch

@Composable
fun PlaylistScreen(
    groups: List<PlaylistGroup>,
    currentPlayingItem: PlaylistItem?,
    onPlayItem: (PlaylistItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onImportUrl: suspend (String) -> Result<List<PlaylistItem>>,
    onImportText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf("全部") }
    var showImportDialog by remember { mutableStateOf(false) }

    val allGroupsList = remember(groups) {
        listOf("全部") + groups.map { it.name }
    }

    // Filter items
    val allItems = remember(groups) { groups.flatMap { it.items } }
    val filteredItems = remember(allItems, searchQuery, selectedGroupFilter) {
        allItems.filter { item ->
            val matchesGroup = selectedGroupFilter == "全部" || item.groupTitle == selectedGroupFilter
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.groupTitle.contains(searchQuery, ignoreCase = true)
            matchesGroup && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuroraDarkBackground)
            .padding(16.dp)
    ) {
        // Title & Import Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "M3U 播放列表与分集",
                    color = AuroraTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "已解析 ${allItems.size} 个流媒体项目 / ${groups.size} 个分组",
                    color = AuroraTextSecondary,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { showImportDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("import_m3u_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = AuroraDarkBackground,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("导入M3U", color = AuroraDarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索频道、剧集名称或分组关键词...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = AuroraTextSecondary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "清空", tint = AuroraTextSecondary)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AuroraCyan,
                unfocusedBorderColor = AuroraBorder,
                focusedTextColor = AuroraTextPrimary,
                unfocusedTextColor = AuroraTextPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("playlist_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Group Filter Horizontal Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(allGroupsList) { groupName ->
                val isSelected = groupName == selectedGroupFilter
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AuroraCyan else AuroraDarkSurface,
                    modifier = Modifier
                        .clickable { selectedGroupFilter = groupName }
                        .testTag("filter_group_$groupName")
                ) {
                    Text(
                        text = groupName,
                        color = if (isSelected) AuroraDarkBackground else AuroraTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stream Items List
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "未搜索到匹配项" else "暂无播放列表，点击上方「导入M3U」添加",
                    color = AuroraTextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    val isCurrentPlaying = item.url == currentPlayingItem?.url
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentPlaying) AuroraCyan.copy(alpha = 0.15f) else AuroraDarkSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayItem(item) }
                            .testTag("playlist_item_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrentPlaying) AuroraCyan else AuroraDarkSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isCurrentPlaying) AuroraDarkBackground else AuroraCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = if (isCurrentPlaying) AuroraCyan else AuroraTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AuroraDarkSurfaceVariant
                                    ) {
                                        Text(
                                            text = item.groupTitle,
                                            color = AuroraTextSecondary,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (item.episodeNumber != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "第 ${item.episodeNumber} 集",
                                            color = AuroraCyan,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Favorite Toggle Button
                            IconButton(
                                onClick = { onToggleFavorite(item.url) },
                                modifier = Modifier.testTag("fav_button_${item.id}")
                            ) {
                                Icon(
                                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (item.isFavorite) "取消收藏" else "收藏",
                                    tint = if (item.isFavorite) AuroraPink else AuroraTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Import M3U Dialog
    if (showImportDialog) {
        var importTab by remember { mutableIntStateOf(0) }
        var importUrl by remember { mutableStateOf("") }
        var importText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isLoading) showImportDialog = false },
            containerColor = AuroraDarkSurface,
            title = {
                Text(
                    text = "导入 M3U / M3U8 播放列表",
                    color = AuroraTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TabRow(
                        selectedTabIndex = importTab,
                        containerColor = AuroraDarkSurfaceVariant,
                        contentColor = AuroraCyan,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[importTab]),
                                color = AuroraCyan
                            )
                        }
                    ) {
                        Tab(
                            selected = importTab == 0,
                            onClick = { importTab = 0 },
                            text = { Text("URL 在线拉取", fontSize = 13.sp) }
                        )
                        Tab(
                            selected = importTab == 1,
                            onClick = { importTab = 1 },
                            text = { Text("粘贴 M3U 文本", fontSize = 13.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (importTab == 0) {
                        OutlinedTextField(
                            value = importUrl,
                            onValueChange = { importUrl = it },
                            label = { Text("M3U 订阅或文件链接 (HTTP/HTTPS)") },
                            placeholder = { Text("https://example.com/live.m3u") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuroraCyan,
                                unfocusedBorderColor = AuroraBorder,
                                focusedTextColor = AuroraTextPrimary,
                                unfocusedTextColor = AuroraTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("import_url_input")
                        )
                    } else {
                        OutlinedTextField(
                            value = importText,
                            onValueChange = { importText = it },
                            label = { Text("直接粘贴 #EXTM3U 文本内容") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuroraCyan,
                                unfocusedBorderColor = AuroraBorder,
                                focusedTextColor = AuroraTextPrimary,
                                unfocusedTextColor = AuroraTextPrimary
                            ),
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth().height(140.dp).testTag("import_text_input")
                        )
                    }

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = errorMsg!!, color = Color.Red, fontSize = 12.sp)
                    }

                    if (isLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(color = AuroraCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在下载并智能分集解析...", color = AuroraCyan, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importTab == 0) {
                            if (importUrl.isNotBlank()) {
                                isLoading = true
                                errorMsg = null
                                scope.launch {
                                    val result = onImportUrl(importUrl.trim())
                                    isLoading = false
                                    if (result.isSuccess) {
                                        showImportDialog = false
                                    } else {
                                        errorMsg = result.exceptionOrNull()?.message ?: "拉取失败"
                                    }
                                }
                            }
                        } else {
                            if (importText.isNotBlank()) {
                                onImportText(importText)
                                showImportDialog = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan),
                    modifier = Modifier.testTag("confirm_import_button")
                ) {
                    Text("开始解析", color = AuroraDarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showImportDialog = false },
                    enabled = !isLoading
                ) {
                    Text("取消")
                }
            }
        )
    }
}
