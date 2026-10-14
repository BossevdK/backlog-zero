package com.backlogzero.app.data

import android.content.Context
import com.backlogzero.app.data.db.UserDao
import com.backlogzero.app.data.db.UserEntity
import com.backlogzero.app.data.db.toDomain
import com.backlogzero.app.domain.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.UUID

/** Fout met een tekst die direct aan de gebruiker getoond mag worden. */
class AuthException(message: String) : Exception(message)

/**
 * Account en sessie. De schermen kennen alleen deze interface; een cloud-backend
 * (Supabase/Firebase) kan later dezelfde interface implementeren.
 */
interface AuthRepository {
    val currentUserId: StateFlow<String?>
    fun observeCurrentUser(): Flow<User?>
    suspend fun register(name: String, email: String, password: String)
    suspend fun login(email: String, password: String)
    fun logout()
    suspend fun deleteAccount()
}

object AuthValidation {
    const val MIN_PASSWORD = 8
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun normalizeEmail(email: String) = email.trim().lowercase()
    fun isValidEmail(email: String) = EMAIL.matches(normalizeEmail(email))
    fun isValidPassword(password: String) = password.length >= MIN_PASSWORD
}

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    var userId: String?
        get() = prefs.getString(KEY, null)
        set(value) = prefs.edit().apply { if (value == null) remove(KEY) else putString(KEY, value) }.apply()

    private companion object {
        const val KEY = "user_id"
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LocalAuthRepository(
    private val users: UserDao,
    private val session: SessionStore,
    private val clock: () -> Long = System::currentTimeMillis,
) : AuthRepository {

    private val _currentUserId = MutableStateFlow(session.userId)
    override val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // Eenvoudige rem tegen gokken: na te veel mislukte pogingen even wachten.
    private var failedAttempts = 0
    private var lockedUntil = 0L

    override fun observeCurrentUser(): Flow<User?> =
        _currentUserId.flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(null) else users.observe(id).map { it?.toDomain() }
        }

    override suspend fun register(name: String, email: String, password: String) {
        val cleanName = name.trim()
        val cleanEmail = AuthValidation.normalizeEmail(email)
        if (cleanName.isEmpty()) throw AuthException("Vul je naam in.")
        if (!AuthValidation.isValidEmail(cleanEmail)) throw AuthException("Dit e-mailadres lijkt niet te kloppen.")
        if (!AuthValidation.isValidPassword(password)) {
            throw AuthException("Kies een wachtwoord van minstens ${AuthValidation.MIN_PASSWORD} tekens.")
        }
        if (users.findByEmail(cleanEmail) != null) throw AuthException("Met dit e-mailadres bestaat al een account.")

        val salt = PasswordHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(password, salt) }
        val user = UserEntity(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            email = cleanEmail,
            passwordHash = hash,
            salt = salt,
            iterations = PasswordHasher.DEFAULT_ITERATIONS,
            createdAt = clock(),
        )
        try {
            users.insert(user)
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            throw AuthException("Met dit e-mailadres bestaat al een account.")
        }
        startSession(user.id)
    }

    override suspend fun login(email: String, password: String) {
        val now = clock()
        if (now < lockedUntil) {
            val seconds = ((lockedUntil - now) / 1000).coerceAtLeast(1)
            throw AuthException("Te veel mislukte pogingen. Probeer het over $seconds seconden opnieuw.")
        }
        val user = users.findByEmail(AuthValidation.normalizeEmail(email))
        val ok = user != null && withContext(Dispatchers.Default) {
            PasswordHasher.verify(password, user.salt, user.iterations, user.passwordHash)
        }
        if (!ok || user == null) {
            failedAttempts++
            if (failedAttempts >= MAX_ATTEMPTS) {
                lockedUntil = clock() + LOCK_MILLIS
                failedAttempts = 0
            }
            throw AuthException("E-mailadres of wachtwoord klopt niet.")
        }
        failedAttempts = 0
        startSession(user.id)
    }

    override fun logout() {
        session.userId = null
        _currentUserId.value = null
    }

    override suspend fun deleteAccount() {
        val id = _currentUserId.value ?: return
        users.delete(id) // items verdwijnen mee via ON DELETE CASCADE
        logout()
    }

    private fun startSession(userId: String) {
        session.userId = userId
        _currentUserId.value = userId
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
        const val LOCK_MILLIS = 30_000L
    }
}
