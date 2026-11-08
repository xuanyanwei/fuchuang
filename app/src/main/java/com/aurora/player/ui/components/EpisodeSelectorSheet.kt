package com.aurora.player.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.aurora.player.ui.theme.AuroraCyan
import com.aurora.player.ui.theme.AuroraDarkBackground
import com.aurora.player.ui.theme.AuroraDarkSurface
import com.aurora.player.ui.theme.AuroraDarkSurfaceVariant
import com.aurora.player.ui.theme.AuroraTextPrimary
import com.aurora.player.ui.theme.AuroraTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeSelectorSheet(
    groups: List<PlaylistGroup>,
    currentPlayingItem: PlaylistItem?,
    onSelectEpisode: (PlaylistItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedGroupName by remember {
        mutableStateOf(currentPlayingItem?.groupTitle ?: groups.firstOrNull()?.name ?: "")
    }

    val activeGroup = groups.firstOrNull { it.name == selectedGroupName } ?: groups.firstOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AuroraDarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "选集与分集列表",
                        color = AuroraTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "共 ${activeGroup?.items?.size ?: 0} 个分集 / 频道",
                        color = AuroraTextSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_episodes_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = AuroraTextSecondary
                    )
                }
            }

            // Group selector tabs (if more than 1 group)
            if (groups.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups) { group ->
                        val isSelected = group.name == selectedGroupName
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) AuroraCyan else AuroraDarkSurfaceVariant,
                            modifier = Modifier
                                .clickable { selectedGroupName = group.name }
                                .testTag("group_tab_${group.name}")
                        ) {
                            Text(
                                text = "${group.name} (${group.items.size})",
                                color = if (isSelected) AuroraDarkBackground else AuroraTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Episodes Grid
            val items = activeGroup?.items ?: emptyList()
            val isAllNumbered = items.isNotEmpty() && items.all { it.episodeNumber != null }

            if (isAllNumbered) {
                // Compact number grid for standard drama episodes (e.g., 01, 02, 03...)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 64.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(260.dp)
                ) {
                    items(items) { item ->
                        val isCurrent = item.url == currentPlayingItem?.url
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) AuroraCyan.copy(alpha = 0.2f) else AuroraDarkSurfaceVariant,
                            border = if (isCurrent) BorderStroke(1.5.dp, AuroraCyan) else null,
                            modifier = Modifier
                                .clickable {
                                    onSelectEpisode(item)
                                    onDismiss()
                                }
                                .testTag("episode_item_${item.episodeNumber}")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.episodeNumber?.let { String.format("%02d", it) } ?: item.title,
                                    color = if (isCurrent) AuroraCyan else AuroraTextPrimary,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // List with titles
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(items) { item ->
                        val isCurrent = item.url == currentPlayingItem?.url
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) AuroraCyan.copy(alpha = 0.15f) else AuroraDarkSurfaceVariant,
                            border = if (isCurrent) BorderStroke(1.dp, if (isCurrent) AuroraCyan else Color.Transparent) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectEpisode(item)
                                    onDismiss()
                                }
                                .testTag("episode_row_${item.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = if (isCurrent) AuroraCyan else AuroraTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.title,
                                    color = if (isCurrent) AuroraCyan else AuroraTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCurrent) {
                                    Text(
                                        text = "正在播放",
                                        color = AuroraCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
