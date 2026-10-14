package com.backlogzero.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {
    @Test
    fun correctPasswordVerifies() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("geheim-wachtwoord", salt, iterations = 1_000)
        assertTrue(PasswordHasher.verify("geheim-wachtwoord", salt, 1_000, hash))
    }

    @Test
    fun wrongPasswordFails() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("geheim-wachtwoord", salt, iterations = 1_000)
        assertFalse(PasswordHasher.verify("Geheim-wachtwoord", salt, 1_000, hash))
    }

    @Test
    fun saltsAreUniqueAndChangeTheHash() {
        val a = PasswordHasher.newSalt()
        val b = PasswordHasher.newSalt()
        assertNotEquals(a, b)
        assertNotEquals(PasswordHasher.hash("x1234567", a, 1_000), PasswordHasher.hash("x1234567", b, 1_000))
    }

    @Test
    fun hashDoesNotContainThePassword() {
        val salt = PasswordHasher.newSalt()
        assertFalse(PasswordHasher.hash("opvallend-wachtwoord", salt, 1_000).contains("opvallend"))
    }
}

class AuthValidationTest {
    @Test
    fun emailChecks() {
        assertTrue(AuthValidation.isValidEmail("  Bosse@Example.nl "))
        assertFalse(AuthValidation.isValidEmail("geen-email"))
        assertFalse(AuthValidation.isValidEmail("a@b"))
        assertEquals("bosse@example.nl", AuthValidation.normalizeEmail("  Bosse@Example.NL "))
    }

    @Test
    fun passwordNeedsEightCharacters() {
        assertFalse(AuthValidation.isValidPassword("kort123"))
        assertTrue(AuthValidation.isValidPassword("langgenoeg"))
    }
}
