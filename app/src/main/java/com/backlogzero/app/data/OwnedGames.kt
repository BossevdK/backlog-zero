package com.backlogzero.app.data

import java.text.Normalizer

/**
 * Maakt titels vergelijkbaar tussen bronnen: zonder hoofdletters, accenten, ™/®-tekens en leestekens.
 * "The Witcher® 3: Wild Hunt" en "the witcher 3 wild hunt" geven dezelfde waarde.
 */
fun normalizeTitle(title: String): String {
    val withoutAccents = Normalizer.normalize(title, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    return withoutAccents.lowercase()
        .replace("&", " and ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
}

/** Markeert resultaten die je al bezit en zet ze bovenaan; de volgorde binnen beide groepen blijft gelijk. */
fun List<SearchResult>.markOwned(ownedNormalizedTitles: Set<String>): List<SearchResult> {
    if (ownedNormalizedTitles.isEmpty()) return this
    return map { it.copy(owned = normalizeTitle(it.title) in ownedNormalizedTitles) }
        .sortedByDescending { it.owned }
}
