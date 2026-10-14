package com.backlogzero.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MetadataParsersTest {

    @Test
    fun `Open Library geeft titel, eerste auteur, jaar en omslag`() {
        val json = """{"docs":[{"title":"Dune","author_name":["Frank Herbert","Ander"],"first_publish_year":1965,"cover_i":123},{"title":""},{"title":"Zonder gegevens"}]}"""
        val r = MetadataParsers.openLibrary(json)
        assertEquals(2, r.size)
        assertEquals(SearchResult("Dune", "Frank Herbert", 1965, "", "https://covers.openlibrary.org/b/id/123-L.jpg"), r[0])
        assertEquals("", r[1].creator)
        assertNull(r[1].year)
        assertNull(r[1].coverUrl)
    }

    @Test
    fun `TMDB film en serie gebruiken hun eigen velden`() {
        val movie = """{"results":[{"id":603,"title":"The Matrix","release_date":"1999-03-31","poster_path":"/p.jpg"}]}"""
        val m = MetadataParsers.tmdb(movie, tv = false).single()
        assertEquals("The Matrix", m.title)
        assertEquals(1999, m.year)
        assertEquals("https://image.tmdb.org/t/p/w500/p.jpg", m.coverUrl)
        assertEquals("603", m.detailId)

        val tv = """{"results":[{"id":1,"name":"Dark","first_air_date":"2017-12-01","poster_path":null}]}"""
        val t = MetadataParsers.tmdb(tv, tv = true).single()
        assertEquals("Dark", t.title)
        assertEquals(2017, t.year)
        assertNull(t.coverUrl)
    }

    @Test
    fun `RAWG geeft platformen en alleen https-omslagen`() {
        val json = """{"results":[
            {"id":7,"name":"Hades","released":"2020-09-17","background_image":"https://media.rawg.io/x.jpg",
             "platforms":[{"platform":{"name":"PC"}},{"platform":{"name":"Switch"}},{"platform":{"name":"PS5"}},{"platform":{"name":"Xbox"}}]},
            {"id":8,"name":"Oud","released":null,"background_image":"http://insecure/x.jpg"}]}"""
        val r = MetadataParsers.rawg(json)
        assertEquals("PC, Switch, PS5", r[0].platform)
        assertEquals(2020, r[0].year)
        assertNull(r[1].coverUrl)
        assertNull(r[1].year)
    }

    @Test
    fun `makers uit de detailantwoorden`() {
        assertEquals(
            "Lana Wachowski",
            MetadataParsers.director("""{"crew":[{"job":"Producer","name":"X"},{"job":"Director","name":"Lana Wachowski"},{"job":"Director","name":"Lana Wachowski"}]}"""),
        )
        assertEquals("A, B", MetadataParsers.creators("""{"created_by":[{"name":"A"},{"name":"B"},{"name":"C"}]}"""))
        assertEquals("Supergiant Games", MetadataParsers.developers("""{"developers":[{"name":"Supergiant Games"}]}"""))
        assertTrue(MetadataParsers.director("{}").isEmpty())
    }
}
