package com.aurora.player.data.model

data class PlaylistItem(
    val id: String,
    val title: String,
    val url: String,
    val durationSeconds: Long = -1L,
    val groupTitle: String = "默认分组",
    val logoUrl: String? = null,
    val episodeNumber: Int? = null,
    val headers: Map<String, String> = emptyMap(),
    val isFavorite: Boolean = false
)

data class PlaylistGroup(
    val name: String,
    val items: List<PlaylistItem>
)

data class PlaybackHistoryItem(
    val id: String,
    val title: String,
    val url: String,
    val groupTitle: String = "默认分组",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val headers: Map<String, String> = emptyMap()
) {
    val progressPercentage: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
