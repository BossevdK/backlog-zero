package com.backlogzero.app.domain

/**
 * Indeling van het dashboard, optioneel beperkt tot één categorie:
 * - Bezig: waar je nu mee bezig bent, laatst bijgewerkt eerst
 * - Volgende uit je backlog: backlog en gepauzeerd, hoogste prioriteit eerst, binnen gelijke prioriteit het langst wachtend eerst
 * - Recent toegevoegd: nieuwste onafgeronde items die nog niet in de rijen hierboven staan
 * - Recent afgerond
 */
data class HomeRows(
    val inProgress: List<MediaItem>,
    val upNext: List<MediaItem>,
    val recentlyAdded: List<MediaItem>,
    val recentlyCompleted: List<MediaItem>,
    val counts: Map<ItemStatus, Int>,
    val total: Int,
) {
    companion object {
        const val ROW_SIZE = 10

        fun of(all: List<MediaItem>, type: MediaType?): HomeRows {
            val items = if (type == null) all else all.filter { it.type == type }

            val inProgress = items.filter { it.status == ItemStatus.IN_PROGRESS }.sortedByDescending { it.updatedAt }
            val upNext = items.filter { it.status == ItemStatus.BACKLOG || it.status == ItemStatus.PAUSED }
                .sortedWith(compareByDescending<MediaItem> { it.priority.level }.thenBy { it.createdAt })
                .take(ROW_SIZE)
            val shown = (inProgress.map { it.id } + upNext.map { it.id }).toSet()
            val recentlyAdded = items.filter { it.status != ItemStatus.COMPLETED && it.id !in shown }
                .sortedByDescending { it.createdAt }.take(ROW_SIZE)
            val recentlyCompleted = items.filter { it.status == ItemStatus.COMPLETED }
                .sortedByDescending { it.completedAt ?: it.updatedAt }.take(ROW_SIZE)

            return HomeRows(
                inProgress = inProgress,
                upNext = upNext,
                recentlyAdded = recentlyAdded,
                recentlyCompleted = recentlyCompleted,
                counts = ItemStatus.entries.associateWith { s -> items.count { it.status == s } },
                total = items.size,
            )
        }
    }
}
