package com.backlogzero.app.domain

data class LibraryFilter(
    val query: String = "",
    val type: MediaType? = null,
    val status: ItemStatus? = null,
    val priority: Priority? = null,
    val favoritesOnly: Boolean = false,
) {
    val isActive: Boolean
        get() = query.isNotBlank() || type != null || status != null || priority != null || favoritesOnly
}

fun List<MediaItem>.filtered(filter: LibraryFilter): List<MediaItem> {
    val q = filter.query.trim()
    return filter { item ->
        (q.isEmpty() || item.title.contains(q, ignoreCase = true)) &&
            (filter.type == null || item.type == filter.type) &&
            (filter.status == null || item.status == filter.status) &&
            (filter.priority == null || item.priority == filter.priority) &&
            (!filter.favoritesOnly || item.favorite)
    }
}
