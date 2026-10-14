package com.backlogzero.app.domain

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class TypeStats(val total: Int, val completed: Int)

data class MonthCount(val month: YearMonth, val completed: Int)

data class LibraryStats(
    val total: Int,
    val byStatus: Map<ItemStatus, Int>,
    val byType: Map<MediaType, TypeStats>,
    /** 0f..1f */
    val completionRate: Float,
    val averageRating: Float?,
    val ratedCount: Int,
    val completedThisYear: Int,
    val favorites: Int,
    /** De laatste zes maanden, oudste eerst. */
    val completedPerMonth: List<MonthCount>,
)

object StatsCalculator {

    fun calculate(items: List<MediaItem>, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LibraryStats {
        val nowMonth = YearMonth.from(Instant.ofEpochMilli(nowMillis).atZone(zone))
        val completed = items.filter { it.status == ItemStatus.COMPLETED }
        val ratings = items.mapNotNull { it.rating }

        val completedMonths = completed.mapNotNull { item ->
            (item.completedAt ?: item.updatedAt).let { YearMonth.from(Instant.ofEpochMilli(it).atZone(zone)) }
        }

        return LibraryStats(
            total = items.size,
            byStatus = ItemStatus.entries.associateWith { s -> items.count { it.status == s } },
            byType = MediaType.entries.associateWith { t ->
                val ofType = items.filter { it.type == t }
                TypeStats(ofType.size, ofType.count { it.status == ItemStatus.COMPLETED })
            },
            completionRate = if (items.isEmpty()) 0f else completed.size.toFloat() / items.size,
            averageRating = if (ratings.isEmpty()) null else ratings.average().toFloat(),
            ratedCount = ratings.size,
            completedThisYear = completedMonths.count { it.year == nowMonth.year },
            favorites = items.count { it.favorite },
            completedPerMonth = (5 downTo 0).map { back ->
                val month = nowMonth.minusMonths(back.toLong())
                MonthCount(month, completedMonths.count { it == month })
            },
        )
    }
}
