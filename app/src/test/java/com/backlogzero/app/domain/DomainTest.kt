package com.backlogzero.app.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun item(
    id: String,
    title: String = id,
    type: MediaType = MediaType.GAME,
    status: ItemStatus = ItemStatus.BACKLOG,
    priority: Priority = Priority.MEDIUM,
    favorite: Boolean = false,
    rating: Int? = null,
    createdAt: Long = 0,
    completedAt: Long? = null,
) = MediaItem(
    id = id, type = type, title = title, status = status, priority = priority, favorite = favorite,
    rating = rating, createdAt = createdAt, updatedAt = createdAt, completedAt = completedAt,
)

class StatusTest {
    @Test
    fun completingSetsCompletionDate() {
        val done = item("a").withStatus(ItemStatus.COMPLETED, now = 500)
        assertEquals(ItemStatus.COMPLETED, done.status)
        assertEquals(500L, done.completedAt)
    }

    @Test
    fun reopeningClearsCompletionDate() {
        val reopened = item("a", status = ItemStatus.COMPLETED, completedAt = 100).withStatus(ItemStatus.PAUSED, now = 900)
        assertNull(reopened.completedAt)
        assertEquals(ItemStatus.PAUSED, reopened.status)
    }

    @Test
    fun sameStatusChangesNothing() {
        val original = item("a", status = ItemStatus.COMPLETED, completedAt = 100)
        assertEquals(original, original.withStatus(ItemStatus.COMPLETED, now = 900))
    }

    @Test
    fun unknownKeysFallBackSafely() {
        assertEquals(MediaType.GAME, MediaType.fromKey("hologram"))
        assertEquals(ItemStatus.BACKLOG, ItemStatus.fromKey("???"))
        assertEquals(Priority.MEDIUM, Priority.fromLevel(42))
    }
}

class FilterTest {
    private val items = listOf(
        item("1", "Elden Ring", MediaType.GAME, ItemStatus.BACKLOG, Priority.HIGH, favorite = true),
        item("2", "Dune", MediaType.BOOK, ItemStatus.IN_PROGRESS, Priority.LOW),
        item("3", "Dune: Part Two", MediaType.MOVIE, ItemStatus.COMPLETED, Priority.MEDIUM),
    )

    @Test
    fun emptyFilterReturnsEverything() {
        assertEquals(3, items.filtered(LibraryFilter()).size)
        assertTrue(!LibraryFilter().isActive)
    }

    @Test
    fun searchIgnoresCase() {
        assertEquals(listOf("2", "3"), items.filtered(LibraryFilter(query = "dUNe")).map { it.id })
    }

    @Test
    fun filtersCombine() {
        assertEquals(listOf("1"), items.filtered(LibraryFilter(type = MediaType.GAME, favoritesOnly = true)).map { it.id })
        assertEquals(listOf("3"), items.filtered(LibraryFilter(query = "dune", status = ItemStatus.COMPLETED)).map { it.id })
        assertEquals(listOf("2"), items.filtered(LibraryFilter(priority = Priority.LOW)).map { it.id })
        assertTrue(items.filtered(LibraryFilter(query = "zzz")).isEmpty())
    }
}

class RecommenderTest {
    private val picker = WeightedRandomRecommender(Random(1))

    @Test
    fun neverPicksCompletedItems() {
        val items = listOf(item("done", status = ItemStatus.COMPLETED), item("todo"))
        repeat(50) { assertEquals("todo", picker.pick(items, PickCriteria())!!.id) }
    }

    @Test
    fun skipsInProgressUnlessAsked() {
        val items = listOf(item("busy", status = ItemStatus.IN_PROGRESS))
        assertNull(picker.pick(items, PickCriteria()))
        assertNotNull(picker.pick(items, PickCriteria(includeInProgress = true)))
    }

    @Test
    fun respectsTypeFilter() {
        val items = listOf(item("g", type = MediaType.GAME), item("b", type = MediaType.BOOK))
        repeat(30) { assertEquals("b", picker.pick(items, PickCriteria(type = MediaType.BOOK))!!.id) }
    }

    @Test
    fun excludesPreviousPickWhenThereIsAChoice() {
        val items = listOf(item("a"), item("b"))
        repeat(30) { assertEquals("b", picker.pick(items, PickCriteria(), exclude = setOf("a"))!!.id) }
    }

    @Test
    fun singleCandidateIsStillReturnedWhenExcluded() {
        assertEquals("a", picker.pick(listOf(item("a")), PickCriteria(), exclude = setOf("a"))!!.id)
    }

    @Test
    fun emptyListGivesNull() {
        assertNull(picker.pick(emptyList(), PickCriteria()))
    }

    @Test
    fun highPriorityIsPickedMoreOften() {
        val items = listOf(item("high", priority = Priority.HIGH), item("low", priority = Priority.LOW))
        val counts = (1..2000).groupingBy { picker.pick(items, PickCriteria())!!.id }.eachCount()
        assertTrue(counts.getValue("high") > counts.getValue("low") * 2)
    }
}

class StatsTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private fun millis(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atStartOfDay(zone).toInstant().toEpochMilli()
    private val now = millis(2026, 10, 14)

    @Test
    fun emptyLibrary() {
        val s = StatsCalculator.calculate(emptyList(), now, zone)
        assertEquals(0, s.total)
        assertEquals(0f, s.completionRate)
        assertNull(s.averageRating)
        assertEquals(6, s.completedPerMonth.size)
    }

    @Test
    fun countsAndAverages() {
        val items = listOf(
            item("1", status = ItemStatus.COMPLETED, rating = 4, completedAt = millis(2026, 10, 2)),
            item("2", status = ItemStatus.COMPLETED, rating = 5, completedAt = millis(2026, 8, 20), type = MediaType.BOOK),
            item("3", status = ItemStatus.COMPLETED, completedAt = millis(2025, 12, 31)),
            item("4", status = ItemStatus.BACKLOG, favorite = true),
        )
        val s = StatsCalculator.calculate(items, now, zone)
        assertEquals(4, s.total)
        assertEquals(0.75f, s.completionRate)
        assertEquals(4.5f, s.averageRating)
        assertEquals(2, s.ratedCount)
        assertEquals(2, s.completedThisYear)
        assertEquals(1, s.favorites)
        assertEquals(3, s.byStatus.getValue(ItemStatus.COMPLETED))
        assertEquals(TypeStats(1, 1), s.byType.getValue(MediaType.BOOK))
        // mei t/m oktober 2026: augustus en oktober hebben elk één afronding
        assertEquals(listOf(0, 0, 0, 1, 0, 1), s.completedPerMonth.map { it.completed })
    }
}
