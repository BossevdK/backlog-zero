package com.backlogzero.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeRowsTest {
    private fun item(
        id: String,
        type: MediaType = MediaType.GAME,
        status: ItemStatus = ItemStatus.BACKLOG,
        priority: Priority = Priority.MEDIUM,
        created: Long = 0,
        updated: Long = created,
        completedAt: Long? = null,
    ) = MediaItem(
        id = id, type = type, title = id, status = status, priority = priority,
        createdAt = created, updatedAt = updated, completedAt = completedAt,
    )

    @Test
    fun `volgende uit je backlog gaat op prioriteit en dan op wachttijd`() {
        val rows = HomeRows.of(
            listOf(
                item("laag-oud", priority = Priority.LOW, created = 1),
                item("hoog-nieuw", priority = Priority.HIGH, created = 9),
                item("hoog-oud", priority = Priority.HIGH, created = 2),
                item("pauze", status = ItemStatus.PAUSED, priority = Priority.MEDIUM, created = 5),
            ),
            type = null,
        )
        assertEquals(listOf("hoog-oud", "hoog-nieuw", "pauze", "laag-oud"), rows.upNext.map { it.id })
    }

    @Test
    fun `recent toegevoegd herhaalt niet wat al in de rijen erboven staat`() {
        val many = (1..12).map { item("b$it", created = it.toLong(), priority = Priority.HIGH) }
        val extra = item("laag", priority = Priority.LOW, created = 100)
        val rows = HomeRows.of(many + extra + item("bezig", status = ItemStatus.IN_PROGRESS, created = 50), type = null)

        assertEquals(HomeRows.ROW_SIZE, rows.upNext.size)
        val shown = (rows.inProgress + rows.upNext).map { it.id }.toSet()
        assertTrue(rows.recentlyAdded.none { it.id in shown })
        assertTrue(rows.recentlyAdded.any { it.id == "laag" })
    }

    @Test
    fun `categorie beperkt alle rijen en tellers`() {
        val items = listOf(
            item("g1", MediaType.GAME),
            item("f1", MediaType.MOVIE, ItemStatus.IN_PROGRESS),
            item("f2", MediaType.MOVIE, ItemStatus.COMPLETED, completedAt = 5),
        )
        val rows = HomeRows.of(items, MediaType.MOVIE)
        assertEquals(listOf("f1"), rows.inProgress.map { it.id })
        assertEquals(emptyList(), rows.upNext)
        assertEquals(listOf("f2"), rows.recentlyCompleted.map { it.id })
        assertEquals(2, rows.total)
        assertEquals(0, rows.counts[ItemStatus.BACKLOG])
    }

    @Test
    fun `recent afgerond gaat op afrondingsdatum`() {
        val rows = HomeRows.of(
            listOf(
                item("oud", status = ItemStatus.COMPLETED, completedAt = 10),
                item("nieuw", status = ItemStatus.COMPLETED, completedAt = 99),
            ),
            type = null,
        )
        assertEquals(listOf("nieuw", "oud"), rows.recentlyCompleted.map { it.id })
    }
}
