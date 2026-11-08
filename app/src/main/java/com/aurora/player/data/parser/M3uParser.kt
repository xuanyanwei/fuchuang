package com.aurora.player.data.parser

import com.aurora.player.data.model.PlaylistItem
import java.util.UUID
import java.util.regex.Pattern

object M3uParser {

    private val ATTRIBUTE_PATTERN = Pattern.compile("([a-zA-Z0-9_-]+)=\"([^\"]*)\"")
    private val EPISODE_PATTERNS = listOf(
        Pattern.compile("第\\s*(\\d+)\\s*[集话回期]"),
        Pattern.compile("(?i)EP?\\s*(\\d+)"),
        Pattern.compile("(?i)Episode\\s*(\\d+)"),
        Pattern.compile("[\\-_#]\\s*(\\d+)(?:[\\s.\\[(]|$)"),
        Pattern.compile("\\s+(\\d+)\\s*$")
    )

    fun parse(content: String, baseUrl: String? = null, defaultHeaders: Map<String, String> = emptyMap()): List<PlaylistItem> {
        val lines = content.lines()
        val items = mutableListOf<PlaylistItem>()

        var currentTitle = ""
        var currentDuration = -1L
        var currentGroup = "默认分组"
        var currentLogo: String? = null
        var currentAttributes = mutableMapOf<String, String>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                // Parse #EXTINF
                val colonIndex = line.indexOf(':')
                val rest = line.substring(colonIndex + 1)
                val commaIndex = rest.indexOf(',')

                val headerPart = if (commaIndex != -1) rest.substring(0, commaIndex) else rest
                currentTitle = if (commaIndex != -1) rest.substring(commaIndex + 1).trim() else "未知流媒体"

                // Duration
                val durationMatch = Regex("^-?\\d+").find(headerPart.trim())
                currentDuration = durationMatch?.value?.toLongOrNull() ?: -1L

                // Attributes
                currentAttributes.clear()
                val matcher = ATTRIBUTE_PATTERN.matcher(headerPart)
                while (matcher.find()) {
                    val key = matcher.group(1)?.lowercase() ?: ""
                    val value = matcher.group(2) ?: ""
                    currentAttributes[key] = value
                }

                currentGroup = currentAttributes["group-title"]?.ifBlank { null }
                    ?: currentAttributes["group"]?.ifBlank { null }
                    ?: detectGroupFromTitle(currentTitle)

                currentLogo = currentAttributes["tvg-logo"]?.ifBlank { null }
                    ?: currentAttributes["logo"]?.ifBlank { null }

            } else if (!line.startsWith("#")) {
                // Stream URL
                var streamUrl = line
                if (baseUrl != null && !streamUrl.startsWith("http://") && !streamUrl.startsWith("https://")) {
                    streamUrl = resolveUrl(baseUrl, streamUrl)
                }

                if (currentTitle.isEmpty()) {
                    currentTitle = extractTitleFromUrl(streamUrl)
                    currentGroup = detectGroupFromTitle(currentTitle)
                }

                val epNumber = detectEpisodeNumber(currentTitle)

                items.add(
                    PlaylistItem(
                        id = UUID.randomUUID().toString(),
                        title = currentTitle,
                        url = streamUrl,
                        durationSeconds = currentDuration,
                        groupTitle = currentGroup,
                        logoUrl = currentLogo,
                        episodeNumber = epNumber,
                        headers = defaultHeaders
                    )
                )

                // Reset for next entry
                currentTitle = ""
                currentDuration = -1L
                currentGroup = "默认分组"
                currentLogo = null
                currentAttributes.clear()
            }
        }

        // If not in M3U format (e.g. user pasted raw URLs)
        if (items.isEmpty() && content.isNotBlank()) {
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    val title = extractTitleFromUrl(trimmed)
                    items.add(
                        PlaylistItem(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            url = trimmed,
                            groupTitle = "导入流",
                            episodeNumber = detectEpisodeNumber(title),
                            headers = defaultHeaders
                        )
                    )
                }
            }
        }

        return items
    }

    private fun detectEpisodeNumber(title: String): Int? {
        for (pattern in EPISODE_PATTERNS) {
            val matcher = pattern.matcher(title)
            if (matcher.find()) {
                val numStr = matcher.group(1)
                numStr?.toIntOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun detectGroupFromTitle(title: String): String {
        return when {
            title.contains("CCTV", ignoreCase = true) -> "央视频道"
            title.contains("卫视") -> "地方卫视"
            title.contains("4K", ignoreCase = true) || title.contains("HDR", ignoreCase = true) -> "超清画质"
            title.contains("电影") || title.contains("Movie", ignoreCase = true) -> "电影放映"
            title.contains("剧") || title.contains("第") && title.contains("集") -> "精选剧集"
            title.contains("动画") || title.contains("动漫") || title.contains("Anime", ignoreCase = true) -> "动漫频道"
            else -> "默认分组"
        }
    }

    private fun extractTitleFromUrl(url: String): String {
        return try {
            val path = url.substringBefore('?').substringBefore('#')
            val filename = path.substringAfterLast('/')
            if (filename.isNotBlank()) {
                filename.substringBeforeLast('.')
            } else {
                "流媒体 #${url.hashCode().toString().takeLast(4)}"
            }
        } catch (_: Exception) {
            "流媒体"
        }
    }

    private fun resolveUrl(base: String, relative: String): String {
        return try {
            val baseUrl = java.net.URL(base)
            java.net.URL(baseUrl, relative).toString()
        } catch (_: Exception) {
            relative
        }
    }
}
