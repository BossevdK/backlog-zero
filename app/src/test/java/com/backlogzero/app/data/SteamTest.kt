package com.backlogzero.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TitleMatchingTest {
    @Test
    fun `titels worden vergelijkbaar gemaakt`() {
        assertEquals("the witcher 3 wild hunt", normalizeTitle("The Witcher® 3: Wild Hunt"))
        assertEquals("the witcher 3 wild hunt", normalizeTitle("  the WITCHER 3 - wild hunt "))
        assertEquals("pokemon", normalizeTitle("Pokémon"))
        assertEquals("tom and jerry", normalizeTitle("Tom & Jerry"))
        assertEquals("half life 2", normalizeTitle("Half-Life 2™"))
    }

    @Test
    fun `eigen games komen bovenaan met behoud van volgorde`() {
        val results = listOf(
            SearchResult("Hades II"),
            SearchResult("Hades"),
            SearchResult("Celeste"),
            SearchResult("Dead Cells"),
        )
        val owned = setOf(normalizeTitle("hades"), normalizeTitle("Dead Cells"))
        val marked = results.markOwned(owned)

        assertEquals(listOf("Hades", "Dead Cells", "Hades II", "Celeste"), marked.map { it.title })
        assertEquals(listOf(true, true, false, false), marked.map { it.owned })
    }

    @Test
    fun `zonder bibliotheek verandert er niets`() {
        val results = listOf(SearchResult("A"), SearchResult("B"))
        assertEquals(results, results.markOwned(emptySet()))
        assertTrue(results.markOwned(emptySet()).none { it.owned })
    }
}

class SteamParsersTest {
    @Test
    fun `games worden uit het antwoord gelezen en rommel overgeslagen`() {
        val json = """{"response":{"game_count":3,"games":[
            {"appid":620,"name":"Portal 2"},
            {"appid":0,"name":"Zonder id"},
            {"appid":730,"name":""},
            {"appid":1145360,"name":"Hades"}]}}"""
        val games = SteamParsers.ownedGames(json)!!
        assertEquals(listOf(SteamGame(620, "Portal 2"), SteamGame(1145360, "Hades")), games)
    }

    @Test
    fun `een privéprofiel geeft null, een leeg account een lege lijst`() {
        assertNull(SteamParsers.ownedGames("""{"response":{}}"""))
        assertNull(SteamParsers.ownedGames("{}"))
        assertEquals(emptyList(), SteamParsers.ownedGames("""{"response":{"game_count":0}}"""))
    }

    @Test
    fun `profielnaam wordt omgezet naar een steam-id`() {
        assertEquals("76561197960287930", SteamParsers.vanity("""{"response":{"steamid":"76561197960287930","success":1}}"""))
        assertNull(SteamParsers.vanity("""{"response":{"success":42,"message":"No match"}}"""))
    }

    @Test
    fun `steam-id herkennen`() {
        assertTrue(SteamParsers.isSteamId("76561197960287930"))
        assertFalse(SteamParsers.isSteamId("gabelogannewell"))
        assertFalse(SteamParsers.isSteamId("1234"))
    }
}
