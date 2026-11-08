package com.aurora.player.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.aurora.player.data.model.PlaybackHistoryItem
import com.aurora.player.data.model.PlaylistGroup
import com.aurora.player.data.model.PlaylistItem
import com.aurora.player.data.model.StreamHeaderConfig
import com.aurora.player.data.model.UserAgentPreset
import com.aurora.player.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class PlaylistRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aurora_player_prefs", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _playlistItems = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val playlistItems: StateFlow<List<PlaylistItem>> = _playlistItems.asStateFlow()

    private val _history = MutableStateFlow<List<PlaybackHistoryItem>>(emptyList())
    val history: StateFlow<List<PlaybackHistoryItem>> = _history.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _headerConfig = MutableStateFlow(StreamHeaderConfig())
    val headerConfig: StateFlow<StreamHeaderConfig> = _headerConfig.asStateFlow()

    init {
        loadSavedData()
        if (_playlistItems.value.isEmpty()) {
            loadDemoPlaylists()
        }
    }

    fun updateHeaderConfig(config: StreamHeaderConfig) {
        _headerConfig.value = config
        saveHeaderConfig(config)
    }

    suspend fun fetchRemotePlaylist(url: String, customHeaders: Map<String, String> = emptyMap()): Result<List<PlaylistItem>> =
        withContext(Dispatchers.IO) {
            try {
                val effectiveHeaders = _headerConfig.value.toHeaderMap().toMutableMap().apply {
                    putAll(customHeaders)
                }

                val reqBuilder = Request.Builder().url(url)
                effectiveHeaders.forEach { (k, v) ->
                    reqBuilder.addHeader(k, v)
                }

                val response = okHttpClient.newCall(reqBuilder.build()).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code}: ${response.message}")
                    )
                }

                val body = response.body?.string() ?: ""
                val items = M3uParser.parse(body, url, effectiveHeaders)
                _playlistItems.value = items
                savePlaylistItems(items)
                Result.success(items)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun importLocalM3uContent(content: String, customHeaders: Map<String, String> = emptyMap()) {
        val items = M3uParser.parse(content, null, customHeaders)
        _playlistItems.value = items
        savePlaylistItems(items)
    }

    fun addSingleStream(title: String, url: String, group: String = "自定义流", customHeaders: Map<String, String> = emptyMap()): PlaylistItem {
        val item = PlaylistItem(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "自定义流 #${System.currentTimeMillis() % 1000}" },
            url = url,
            groupTitle = group,
            headers = customHeaders.ifEmpty { _headerConfig.value.toHeaderMap() }
        )
        val current = _playlistItems.value.toMutableList()
        current.add(0, item)
        _playlistItems.value = current
        savePlaylistItems(current)
        return item
    }

    fun recordHistory(item: PlaylistItem, positionMs: Long, durationMs: Long) {
        val current = _history.value.toMutableList()
        current.removeAll { it.url == item.url }
        current.add(
            0,
            PlaybackHistoryItem(
                id = UUID.randomUUID().toString(),
                title = item.title,
                url = item.url,
                groupTitle = item.groupTitle,
                positionMs = positionMs,
                durationMs = durationMs,
                timestamp = System.currentTimeMillis(),
                headers = item.headers
            )
        )
        // Keep max 50 items
        val trimmed = current.take(50)
        _history.value = trimmed
        saveHistory(trimmed)
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove("history_json").apply()
    }

    fun toggleFavorite(url: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(url)) {
            current.remove(url)
        } else {
            current.add(url)
        }
        _favorites.value = current
        prefs.edit().putStringSet("favorites_set", current).apply()
    }

    fun getGroupedPlaylist(): List<PlaylistGroup> {
        val favs = _favorites.value
        val items = _playlistItems.value.map { it.copy(isFavorite = favs.contains(it.url)) }
        val groups = items.groupBy { it.groupTitle }
        return groups.map { (groupName, groupItems) ->
            PlaylistGroup(groupName, groupItems.sortedBy { it.episodeNumber ?: Int.MAX_VALUE })
        }
    }

    private fun loadDemoPlaylists() {
        val demos = listOf(
            PlaylistItem(
                id = "demo_aurora_4k",
                title = "极光超清测试流 (4K HLS)",
                url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                groupTitle = "官方演示流",
                episodeNumber = 1
            ),
            PlaylistItem(
                id = "demo_bbb",
                title = "大雄兔 (Big Buck Bunny - 多码率HLS)",
                url = "https://test-streams.mux.dev/test_001/stream.m3u8",
                groupTitle = "官方演示流",
                episodeNumber = 2
            ),
            PlaylistItem(
                id = "demo_tears_of_steel",
                title = "钢铁之泪 (Tears of Steel 1080P)",
                url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                groupTitle = "官方演示流",
                episodeNumber = 3
            ),
            PlaylistItem(
                id = "demo_sintel",
                title = "寻梦科幻 (Sintel Animation 1080P)",
                url = "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
                groupTitle = "官方演示流",
                episodeNumber = 4
            ),
            PlaylistItem(
                id = "demo_apple_bipbop",
                title = "苹果官方 HLS 基准流 (BipBop 16:9)",
                url = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
                groupTitle = "技术验证流",
                episodeNumber = 1
            ),
            // Multi-episode Drama Demo (分集示例)
            PlaylistItem(
                id = "ep_demo_01",
                title = "极光探索系列 - 第01集 极光之源",
                url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                groupTitle = "极光纪录片(多集)",
                episodeNumber = 1
            ),
            PlaylistItem(
                id = "ep_demo_02",
                title = "极光探索系列 - 第02集 太阳风暴",
                url = "https://test-streams.mux.dev/test_001/stream.m3u8",
                groupTitle = "极光纪录片(多集)",
                episodeNumber = 2
            ),
            PlaylistItem(
                id = "ep_demo_03",
                title = "极光探索系列 - 第03集 磁层共振",
                url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                groupTitle = "极光纪录片(多集)",
                episodeNumber = 3
            ),
            PlaylistItem(
                id = "ep_demo_04",
                title = "极光探索系列 - 第04集 绚烂终章",
                url = "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
                groupTitle = "极光纪录片(多集)",
                episodeNumber = 4
            )
        )
        _playlistItems.value = demos
        savePlaylistItems(demos)
    }

    private fun loadSavedData() {
        val favs = prefs.getStringSet("favorites_set", emptySet()) ?: emptySet()
        _favorites.value = favs

        val itemsJson = prefs.getString("playlist_items_json", null)
        if (!itemsJson.isNullOrBlank()) {
            try {
                val array = JSONArray(itemsJson)
                val items = mutableListOf<PlaylistItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    items.add(
                        PlaylistItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", ""),
                            url = obj.optString("url", ""),
                            durationSeconds = obj.optLong("durationSeconds", -1L),
                            groupTitle = obj.optString("groupTitle", "默认分组"),
                            logoUrl = obj.optString("logoUrl").ifBlank { null },
                            episodeNumber = if (obj.has("episodeNumber")) obj.getInt("episodeNumber") else null
                        )
                    )
                }
                _playlistItems.value = items
            } catch (_: Exception) {}
        }

        val historyJson = prefs.getString("history_json", null)
        if (!historyJson.isNullOrBlank()) {
            try {
                val array = JSONArray(historyJson)
                val list = mutableListOf<PlaybackHistoryItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PlaybackHistoryItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", ""),
                            url = obj.optString("url", ""),
                            groupTitle = obj.optString("groupTitle", "默认分组"),
                            positionMs = obj.optLong("positionMs", 0L),
                            durationMs = obj.optLong("durationMs", 0L),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                _history.value = list
            } catch (_: Exception) {}
        }

        val uaPreset = prefs.getString("ua_preset", UserAgentPreset.CHROME_PC.name)
        val customUa = prefs.getString("custom_ua", "") ?: ""
        val referer = prefs.getString("header_referer", "") ?: ""
        val origin = prefs.getString("header_origin", "") ?: ""
        val cookie = prefs.getString("header_cookie", "") ?: ""
        val auth = prefs.getString("header_auth", "") ?: ""
        _headerConfig.value = StreamHeaderConfig(
            userAgentType = try { UserAgentPreset.valueOf(uaPreset ?: "") } catch (_: Exception) { UserAgentPreset.CHROME_PC },
            customUserAgent = customUa,
            referer = referer,
            origin = origin,
            cookie = cookie,
            authorization = auth
        )
    }

    private fun savePlaylistItems(items: List<PlaylistItem>) {
        try {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("url", item.url)
                    put("durationSeconds", item.durationSeconds)
                    put("groupTitle", item.groupTitle)
                    item.logoUrl?.let { put("logoUrl", it) }
                    item.episodeNumber?.let { put("episodeNumber", it) }
                }
                array.put(obj)
            }
            prefs.edit().putString("playlist_items_json", array.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun saveHistory(list: List<PlaybackHistoryItem>) {
        try {
            val array = JSONArray()
            for (h in list) {
                val obj = JSONObject().apply {
                    put("id", h.id)
                    put("title", h.title)
                    put("url", h.url)
                    put("groupTitle", h.groupTitle)
                    put("positionMs", h.positionMs)
                    put("durationMs", h.durationMs)
                    put("timestamp", h.timestamp)
                }
                array.put(obj)
            }
            prefs.edit().putString("history_json", array.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun saveHeaderConfig(config: StreamHeaderConfig) {
        prefs.edit()
            .putString("ua_preset", config.userAgentType.name)
            .putString("custom_ua", config.customUserAgent)
            .putString("header_referer", config.referer)
            .putString("header_origin", config.origin)
            .putString("header_cookie", config.cookie)
            .putString("header_auth", config.authorization)
            .apply()
    }
}
