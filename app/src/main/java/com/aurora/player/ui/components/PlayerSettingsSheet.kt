package com.aurora.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.player.player.AspectRatioMode
import com.aurora.player.player.AuroraPlayerController
import com.aurora.player.player.PlayerState
import com.aurora.player.ui.theme.AuroraBlack
import com.aurora.player.ui.theme.AuroraDarkSurface
import com.aurora.player.ui.theme.AuroraDarkSurfaceVariant
import com.aurora.player.ui.theme.AuroraOrange
import com.aurora.player.ui.theme.AuroraTextPrimary
import com.aurora.player.ui.theme.AuroraTextSecondary

data class PlayerSettingsConfig(
    val forceFillScreen: Boolean = false,
    val coverStretch: Boolean = false,
    val autoOrientation: Boolean = true,
    val loopPlayback: Boolean = false,
    val randomPlay: Boolean = false,
    val autoNextEpisode: Boolean = true,
    val sleepTimerMinutes: Int = 0 // 0 means not set
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsSheet(
    playerState: PlayerState,
    settingsConfig: PlayerSettingsConfig,
    onUpdateConfig: (PlayerSettingsConfig) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetAspectRatio: (AspectRatioMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161822),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = AuroraOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "播放器设置",
                        color = AuroraTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = AuroraTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SECTION 1: 显示 ---
            Text(
                text = "显示",
                color = AuroraTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AuroraDarkSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    SettingToggleRow(
                        icon = Icons.Default.FitScreen,
                        title = "强制铺满全屏 (Fill)",
                        checked = settingsConfig.forceFillScreen,
                        onCheckedChange = { checked ->
                            onUpdateConfig(settingsConfig.copy(forceFillScreen = checked))
                            if (checked) {
                                onSetAspectRatio(AspectRatioMode.FILL)
                            } else {
                                onSetAspectRatio(AspectRatioMode.FIT)
                            }
                        }
                    )
                    SettingDivider()
                    SettingToggleRow(
                        icon = Icons.Default.OpenInFull,
                        title = "拉伸以铺满全屏 (Cover)",
                        checked = settingsConfig.coverStretch,
                        onCheckedChange = { checked ->
                            onUpdateConfig(settingsConfig.copy(coverStretch = checked))
                            if (checked) {
                                onSetAspectRatio(AspectRatioMode.FILL)
                            } else {
                                onSetAspectRatio(AspectRatioMode.FIT)
                            }
                        }
                    )
                    SettingDivider()
                    SettingToggleRow(
                        icon = Icons.Default.StayCurrentPortrait,
                        title = "自适应横竖屏",
                        checked = settingsConfig.autoOrientation,
                        onCheckedChange = { checked ->
                            onUpdateConfig(settingsConfig.copy(autoOrientation = checked))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- SECTION 2: 播放 ---
            Text(
                text = "播放",
                color = AuroraTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AuroraDarkSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    SettingToggleRow(
                        icon = Icons.Default.Repeat,
                        title = "开启循环播放",
                        checked = settingsConfig.loopPlayback,
                        onCheckedChange = { checked ->
                            onUpdateConfig(settingsConfig.copy(loopPlayback = checked))
                        }
                    )
                    SettingDivider()
                    SettingToggleRow(
                        icon = Icons.Default.Shuffle,
                        title = "开启随机连播",
                        checked = settingsConfig.randomPlay,
                        onCheckedChange = { checked ->
                            onUpdateConfig(settingsConfig.copy(randomPlay = checked))
                        }
                    )
                    SettingDivider()
                    SettingToggleRow(
                        icon = Icons.Default.SkipNext,
                        title = "自动播放下一集",
                        checked = settingsConfig.autoNextEpisode,
                        onCheckedChange = { checked ->
                            onUpdateConfig(settingsConfig.copy(autoNextEpisode = checked))
                        }
                    )
                    SettingDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Cycle sleep timer: 0 -> 15 -> 30 -> 60 -> 0
                                val next = when (settingsConfig.sleepTimerMinutes) {
                                    0 -> 15
                                    15 -> 30
                                    30 -> 60
                                    else -> 0
                                }
                                onUpdateConfig(settingsConfig.copy(sleepTimerMinutes = next))
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = AuroraTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "定时关闭",
                                color = AuroraTextPrimary,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = if (settingsConfig.sleepTimerMinutes > 0) "${settingsConfig.sleepTimerMinutes}分钟后 >" else "未设置 >",
                            color = if (settingsConfig.sleepTimerMinutes > 0) AuroraOrange else AuroraTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- SECTION 3: 倍速播放 ---
            Text(
                text = "倍速播放",
                color = AuroraTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(10.dp))

            val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(speedOptions) { speed ->
                    val isSelected = (playerState.playbackSpeed == speed)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) AuroraOrange else AuroraDarkSurfaceVariant,
                        modifier = Modifier
                            .clickable { onSetPlaybackSpeed(speed) }
                            .testTag("speed_${speed}x")
                    ) {
                        Text(
                            text = "${speed}x",
                            color = if (isSelected) AuroraBlack else AuroraTextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AuroraTextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = AuroraTextPrimary,
                fontSize = 14.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AuroraBlack,
                checkedTrackColor = AuroraOrange,
                uncheckedThumbColor = AuroraTextSecondary,
                uncheckedTrackColor = Color(0xFF282B37)
            )
        )
    }
}

@Composable
private fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color(0xFF262A38))
    )
}
