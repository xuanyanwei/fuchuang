package com.aurora.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.aurora.player.data.model.UserAgentPreset
import com.aurora.player.ui.theme.AuroraBorder
import com.aurora.player.ui.theme.AuroraCyan
import com.aurora.player.ui.theme.AuroraDarkBackground
import com.aurora.player.ui.theme.AuroraDarkSurface
import com.aurora.player.ui.theme.AuroraDarkSurfaceVariant
import com.aurora.player.ui.theme.AuroraError
import com.aurora.player.ui.theme.AuroraTextPrimary
import com.aurora.player.ui.theme.AuroraTextSecondary

@Composable
fun HeaderConfigDialog(
    initialConfig: StreamHeaderConfig,
    onSave: (StreamHeaderConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember { mutableStateOf(initialConfig.userAgentType) }
    var customUa by remember { mutableStateOf(initialConfig.customUserAgent) }
    var referer by remember { mutableStateOf(initialConfig.referer) }
    var origin by remember { mutableStateOf(initialConfig.origin) }
    var cookie by remember { mutableStateOf(initialConfig.cookie) }
    var authorization by remember { mutableStateOf(initialConfig.authorization) }
    var showPresetMenu by remember { mutableStateOf(false) }

    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    val customPairs = remember {
        mutableStateMapOf<String, String>().apply {
            putAll(initialConfig.customHeaders)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AuroraDarkSurface,
        title = {
            Text(
                text = "自定义 HTTP 请求头设置",
                color = AuroraTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "针对防盗链、CORS 跨域及鉴权流媒体，自动注入指定请求头。",
                    color = AuroraTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // User-Agent Preset
                Text(
                    text = "User-Agent 预设",
                    color = AuroraCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AuroraDarkSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPresetMenu = true }
                        .testTag("select_ua_preset_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = selectedPreset.displayName, color = AuroraTextPrimary, fontSize = 14.sp)
                        Text(text = "切换 ▾", color = AuroraCyan, fontSize = 12.sp)
                    }

                    DropdownMenu(
                        expanded = showPresetMenu,
                        onDismissRequest = { showPresetMenu = false }
                    ) {
                        UserAgentPreset.entries.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.displayName) },
                                onClick = {
                                    selectedPreset = preset
                                    showPresetMenu = false
                                }
                            )
                        }
                    }
                }

                if (selectedPreset == UserAgentPreset.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customUa,
                        onValueChange = { customUa = it },
                        label = { Text("输入自定义 User-Agent") },
                        colors = outlinedColors(),
                        modifier = Modifier.fillMaxWidth().testTag("custom_ua_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Referer
                OutlinedTextField(
                    value = referer,
                    onValueChange = { referer = it },
                    label = { Text("Referer (防盗链来源)") },
                    placeholder = { Text("https://example.com/") },
                    colors = outlinedColors(),
                    modifier = Modifier.fillMaxWidth().testTag("referer_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Origin
                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("Origin (来源地址)") },
                    placeholder = { Text("https://example.com") },
                    colors = outlinedColors(),
                    modifier = Modifier.fillMaxWidth().testTag("origin_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Cookie
                OutlinedTextField(
                    value = cookie,
                    onValueChange = { cookie = it },
                    label = { Text("Cookie") },
                    placeholder = { Text("session_id=...; token=...") },
                    colors = outlinedColors(),
                    modifier = Modifier.fillMaxWidth().testTag("cookie_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Authorization
                OutlinedTextField(
                    value = authorization,
                    onValueChange = { authorization = it },
                    label = { Text("Authorization (令牌认证)") },
                    placeholder = { Text("Bearer eyJhbGciOi...") },
                    colors = outlinedColors(),
                    modifier = Modifier.fillMaxWidth().testTag("auth_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Extra Headers
                Text(
                    text = "额外自定义头 (Key-Value)",
                    color = AuroraCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                customPairs.forEach { (k, v) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$k: $v",
                            color = AuroraTextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { customPairs.remove(k) },
                            modifier = Modifier.testTag("delete_header_$k")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = AuroraError
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("名称 Key") },
                        colors = outlinedColors(),
                        modifier = Modifier.weight(1f).testTag("new_header_key")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("值 Value") },
                        colors = outlinedColors(),
                        modifier = Modifier.weight(1f).testTag("new_header_val")
                    )
                    IconButton(
                        onClick = {
                            if (newKey.isNotBlank() && newValue.isNotBlank()) {
                                customPairs[newKey.trim()] = newValue.trim()
                                newKey = ""
                                newValue = ""
                            }
                        },
                        modifier = Modifier.testTag("add_header_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "添加", tint = AuroraCyan)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val config = StreamHeaderConfig(
                        userAgentType = selectedPreset,
                        customUserAgent = customUa,
                        referer = referer.trim(),
                        origin = origin.trim(),
                        cookie = cookie.trim(),
                        authorization = authorization.trim(),
                        customHeaders = customPairs.toMap()
                    )
                    onSave(config)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan),
                modifier = Modifier.testTag("save_header_config_button")
            ) {
                Text("保存设置", color = AuroraDarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AuroraTextSecondary)
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AuroraCyan,
    unfocusedBorderColor = AuroraBorder,
    focusedLabelColor = AuroraCyan,
    unfocusedLabelColor = AuroraTextSecondary,
    focusedTextColor = AuroraTextPrimary,
    unfocusedTextColor = AuroraTextPrimary
)
