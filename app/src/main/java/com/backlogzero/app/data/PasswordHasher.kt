package com.backlogzero.app.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Wachtwoorden worden nooit opgeslagen, alleen een PBKDF2-hash met een eigen zout. */
object PasswordHasher {
    const val DEFAULT_ITERATIONS = 150_000
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(password: String, salt: String, iterations: Int = DEFAULT_ITERATIONS): String {
        val spec = PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), iterations, KEY_BITS)
        try {
            val key = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
            return Base64.getEncoder().encodeToString(key)
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(password: String, salt: String, iterations: Int, expectedHash: String): Boolean {
        val actual = hash(password, salt, iterations)
        return MessageDigest.isEqual(actual.toByteArray(), expectedHash.toByteArray())
    }
}
