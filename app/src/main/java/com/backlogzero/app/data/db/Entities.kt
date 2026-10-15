package com.backlogzero.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.backlogzero.app.domain.ItemStatus
import com.backlogzero.app.domain.MediaItem
import com.backlogzero.app.domain.MediaType
import com.backlogzero.app.domain.Priority
import com.backlogzero.app.domain.User

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val iterations: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("userId")],
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val creator: String,
    val platform: String,
    val releaseYear: Int?,
    val coverFile: String?,
    val notes: String,
    val rating: Int?,
    val status: String,
    val priority: Int,
    val favorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
)

fun UserEntity.toDomain() = User(id = id, name = name, email = email)

fun ItemEntity.toDomain() = MediaItem(
    id = id,
    type = MediaType.fromKey(type),
    title = title,
    creator = creator,
    platform = platform,
    releaseYear = releaseYear,
    coverFile = coverFile,
    notes = notes,
    rating = rating,
    status = ItemStatus.fromKey(status),
    priority = Priority.fromLevel(priority),
    favorite = favorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
)

fun MediaItem.toEntity(userId: String) = ItemEntity(
    id = id,
    userId = userId,
    type = type.key,
    title = title,
    creator = creator,
    platform = platform,
    releaseYear = releaseYear,
    coverFile = coverFile,
    notes = notes,
    rating = rating,
    status = status.key,
    priority = priority.level,
    favorite = favorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
)

/** Een game uit de Steam-bibliotheek van de gebruiker; alleen gebruikt om zoekresultaten te markeren. */
@Entity(
    tableName = "steam_games",
    primaryKeys = ["userId", "appId"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("userId")],
)
data class SteamGameEntity(
    val userId: String,
    val appId: Long,
    val name: String,
    val normalizedName: String,
)
