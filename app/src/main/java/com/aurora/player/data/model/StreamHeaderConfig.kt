package com.aurora.player.data.model

data class StreamHeaderConfig(
    val userAgentType: UserAgentPreset = UserAgentPreset.CHROME_PC,
    val customUserAgent: String = "",
    val referer: String = "",
    val origin: String = "",
    val cookie: String = "",
    val authorization: String = "",
    val customHeaders: Map<String, String> = emptyMap()
) {
    fun toHeaderMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val ua = when (userAgentType) {
            UserAgentPreset.CHROME_PC -> UserAgentPreset.CHROME_PC.value
            UserAgentPreset.SAFARI_MAC -> UserAgentPreset.SAFARI_MAC.value
            UserAgentPreset.ANDROID_TV -> UserAgentPreset.ANDROID_TV.value
            UserAgentPreset.WECHAT -> UserAgentPreset.WECHAT.value
            UserAgentPreset.CUSTOM -> customUserAgent.ifBlank { UserAgentPreset.CHROME_PC.value }
        }
        map["User-Agent"] = ua
        map["Accept"] = "*/*"
        if (referer.isNotBlank()) map["Referer"] = referer
        if (origin.isNotBlank()) map["Origin"] = origin
        if (cookie.isNotBlank()) map["Cookie"] = cookie
        if (authorization.isNotBlank()) map["Authorization"] = authorization
        customHeaders.forEach { (k, v) ->
            if (k.isNotBlank() && v.isNotBlank()) {
                map[k] = v
            }
        }
        return map
    }
}

enum class UserAgentPreset(val displayName: String, val value: String) {
    CHROME_PC(
        "Chrome 电脑端",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    ),
    SAFARI_MAC(
        "Safari 苹果端",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15"
    ),
    ANDROID_TV(
        "Android TV / 原生",
        "Mozilla/5.0 (Linux; Android 12; BRAVIA 4K Build/MMB29M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.101 Safari/537.36 AuroraPlayer/1.0"
    ),
    WECHAT(
        "微信移动内置浏览器",
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/116.0.0.0 Mobile Safari/537.36 MicroMessenger/8.0.48"
    ),
    CUSTOM("自定义 User-Agent", "")
}
