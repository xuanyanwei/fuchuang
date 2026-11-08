package com.aurora.player.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.player.data.model.StreamHeaderConfig
import com.aurora.player.ui.components.HeaderConfigDialog
import com.aurora.player.ui.theme.AuroraCyan
import com.aurora.player.ui.theme.AuroraDarkBackground
import com.aurora.player.ui.theme.AuroraDarkSurface
import com.aurora.player.ui.theme.AuroraDarkSurfaceVariant
import com.aurora.player.ui.theme.AuroraNeonBlue
import com.aurora.player.ui.theme.AuroraTextPrimary
import com.aurora.player.ui.theme.AuroraTextSecondary

@Composable
fun SettingsScreen(
    headerConfig: StreamHeaderConfig,
    onSaveHeaderConfig: (StreamHeaderConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showHeaderDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuroraDarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "网络与播放器设置",
                color = AuroraTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "自定义防盗链头、User-Agent 以及底层 Media3 解码策略",
                color = AuroraTextSecondary,
                fontSize = 12.sp
            )
        }

        // HTTP Headers Card
        Card(
            colors = CardDefaults.cardColors(containerColor = AuroraDarkSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AuroraCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Http,
                                    contentDescription = null,
                                    tint = AuroraCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "HTTP 请求头配置 (Headers)",
                                color = AuroraTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "当前 User-Agent: ${headerConfig.userAgentType.displayName}",
                                color = AuroraTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showHeaderDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_headers_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = AuroraDarkBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("修改设置", color = AuroraDarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AuroraDarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "已配置参数预览:",
                            color = AuroraCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Referer: ${headerConfig.referer.ifBlank { "未设置 (使用默认)" }}",
                            color = AuroraTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• Origin: ${headerConfig.origin.ifBlank { "未设置" }}",
                            color = AuroraTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• Cookie: ${if (headerConfig.cookie.isNotBlank()) "已注入自定义Cookie" else "无"}",
                            color = AuroraTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• 额外自定义头: ${headerConfig.customHeaders.size} 项",
                            color = AuroraTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Stream Relay Architecture Card
        Card(
            colors = CardDefaults.cardColors(containerColor = AuroraDarkSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AuroraNeonBlue.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = AuroraNeonBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "流媒体中继与原生解码策略",
                        color = AuroraTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "极光播放器直接基于 Google AndroidX Media3 (ExoPlayer) 与 OkHttp 底层网路引擎开发。所有 HTTP 请求头会直接在底层 socket 和 TS/M3U8 分片请求中透明注入，无需本地架设 Node.js 代理，提供极致的低延迟、硬解码性能与画质输出。",
                    color = AuroraTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Play Policy & Integrity Card
        Card(
            colors = CardDefaults.cardColors(containerColor = AuroraDarkSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AuroraCyan.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = AuroraCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "纯净体验与合规设计",
                        color = AuroraTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "• 无任何第三方商业广告 SDK，完全符合 Google Play 开发者计划与创作者规范\n• 仅请求 INTERNET 与 ACCESS_NETWORK_STATE 基础网络权限\n• 支持原生 Android Picture-in-Picture 画中画与全屏旋转适配",
                    color = AuroraTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // App About Card
        Card(
            colors = CardDefaults.cardColors(containerColor = AuroraDarkSurfaceVariant),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AuroraCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "关于 极光流媒体播放器",
                        color = AuroraTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "版本: 1.0.0 (Kotlin + Jetpack Compose + Media3)\n平台: Android 7.0 - Android 15+",
                    color = AuroraTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }

    if (showHeaderDialog) {
        HeaderConfigDialog(
            initialConfig = headerConfig,
            onSave = { newConfig ->
                onSaveHeaderConfig(newConfig)
                showHeaderDialog = false
            },
            onDismiss = { showHeaderDialog = false }
        )
    }
}
