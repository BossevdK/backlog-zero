package com.backlogzero.app.data

import com.backlogzero.app.data.db.ItemDao
import com.backlogzero.app.data.db.toDomain
import com.backlogzero.app.data.db.toEntity
import com.backlogzero.app.domain.MediaItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Alle bibliotheekdata van de ingelogde gebruiker. Een synchronisatielaag kan later
 * achter deze interface komen zonder dat de schermen veranderen.
 */
interface LibraryRepository {
    fun observeItems(): Flow<List<MediaItem>>
    fun observeItem(id: String): Flow<MediaItem?>
    suspend fun save(item: MediaItem)
    suspend fun delete(id: String)
    /** Verwijdert alle omslagen van de gebruiker (vóór het verwijderen van het account). */
    suspend fun purgeCovers()
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoomLibraryRepository(
    private val items: ItemDao,
    private val auth: AuthRepository,
    private val covers: CoverStorage,
) : LibraryRepository {

    private fun requireUser(): String =
        auth.currentUserId.value ?: throw IllegalStateException("Niet ingelogd.")

    override fun observeItems(): Flow<List<MediaItem>> =
        auth.currentUserId.flatMapLatest { userId ->
            if (userId == null) emptyFlow() else items.observeAll(userId).map { list -> list.map { it.toDomain() } }
        }

    override fun observeItem(id: String): Flow<MediaItem?> =
        auth.currentUserId.flatMapLatest { userId ->
            if (userId == null) emptyFlow() else items.observe(userId, id).map { it?.toDomain() }
        }

    override suspend fun save(item: MediaItem) {
        items.upsert(item.toEntity(requireUser()))
    }

    override suspend fun delete(id: String) {
        val userId = requireUser()
        val existing = items.find(userId, id)
        items.delete(userId, id)
        covers.delete(existing?.coverFile)
    }

    override suspend fun purgeCovers() {
        items.coverFiles(requireUser()).forEach { covers.delete(it) }
    }
}
