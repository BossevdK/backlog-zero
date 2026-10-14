package com.backlogzero.app.domain

import kotlin.random.Random

data class PickCriteria(
    val type: MediaType? = null,
    /** Ook items meenemen waar je al mee bezig bent. */
    val includeInProgress: Boolean = false,
)

/**
 * Kiest een item voor "Pick for me". Een interface zodat er later een slimmere
 * (bijv. AI-)aanbeveling naast of in plaats van deze kan komen.
 */
interface Recommender {
    fun pick(
        items: List<MediaItem>,
        criteria: PickCriteria,
        exclude: Set<String> = emptySet(),
    ): MediaItem?
}

/**
 * Gewogen toeval: niet-afgeronde items komen in aanmerking, hogere prioriteit
 * heeft meer kans en favorieten krijgen een kleine bonus.
 */
class WeightedRandomRecommender(private val random: Random = Random.Default) : Recommender {

    override fun pick(items: List<MediaItem>, criteria: PickCriteria, exclude: Set<String>): MediaItem? {
        val eligible = items.filter { item ->
            item.status != ItemStatus.COMPLETED &&
                (criteria.includeInProgress || item.status != ItemStatus.IN_PROGRESS) &&
                (criteria.type == null || item.type == criteria.type)
        }
        if (eligible.isEmpty()) return null
        // Bij meer dan één kandidaat willen we bij "een andere" echt iets anders.
        val pool = eligible.filter { it.id !in exclude }.ifEmpty { eligible }
        val weights = pool.map { weight(it) }
        var roll = random.nextInt(weights.sum())
        for ((index, w) in weights.withIndex()) {
            if (roll < w) return pool[index]
            roll -= w
        }
        return pool.last()
    }

    private fun weight(item: MediaItem): Int =
        item.priority.weight * 2 + if (item.favorite) 1 else 0
}
