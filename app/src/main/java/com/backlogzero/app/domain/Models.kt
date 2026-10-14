package com.backlogzero.app.domain

/**
 * Mediatypes. Opgeslagen als tekst-sleutel (`key`), zodat nieuwe categorieën later
 * toegevoegd kunnen worden zonder databasemigratie.
 */
enum class MediaType(
    val key: String,
    val label: String,
    val plural: String,
    val creatorLabel: String,
    val platformLabel: String,
) {
    GAME("game", "Game", "Games", "Studio", "Platform"),
    MOVIE("movie", "Film", "Films", "Regisseur", "Waar te kijken"),
    SERIES("series", "Serie", "Series", "Maker", "Waar te kijken"),
    BOOK("book", "Boek", "Boeken", "Auteur", "Formaat");

    companion object {
        fun fromKey(key: String): MediaType = entries.firstOrNull { it.key == key } ?: GAME
    }
}

enum class ItemStatus(val key: String, val label: String) {
    BACKLOG("backlog", "Backlog"),
    IN_PROGRESS("in_progress", "Bezig"),
    PAUSED("paused", "Gepauzeerd"),
    COMPLETED("completed", "Afgerond");

    companion object {
        fun fromKey(key: String): ItemStatus = entries.firstOrNull { it.key == key } ?: BACKLOG
    }
}

enum class Priority(val level: Int, val label: String, val weight: Int) {
    LOW(1, "Laag", 1),
    MEDIUM(2, "Normaal", 2),
    HIGH(3, "Hoog", 4);

    companion object {
        fun fromLevel(level: Int): Priority = entries.firstOrNull { it.level == level } ?: MEDIUM
    }
}

data class User(
    val id: String,
    val name: String,
    val email: String,
)

data class MediaItem(
    val id: String,
    val type: MediaType,
    val title: String,
    val creator: String = "",
    val platform: String = "",
    val releaseYear: Int? = null,
    /** Bestandsnaam van de omslag in de app-opslag (zie CoverStorage), of null. */
    val coverFile: String? = null,
    val notes: String = "",
    /** Persoonlijke beoordeling 1..5, of null als nog niet beoordeeld. */
    val rating: Int? = null,
    val status: ItemStatus = ItemStatus.BACKLOG,
    val priority: Priority = Priority.MEDIUM,
    val favorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
)

/** Zet een nieuwe status en houdt `completedAt` consistent. */
fun MediaItem.withStatus(newStatus: ItemStatus, now: Long): MediaItem {
    if (newStatus == status) return this
    val completed = when {
        newStatus != ItemStatus.COMPLETED -> null
        else -> completedAt ?: now
    }
    return copy(status = newStatus, completedAt = completed, updatedAt = now)
}
