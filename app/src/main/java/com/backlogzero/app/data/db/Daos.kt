package com.backlogzero.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observe(id: String): Flow<UserEntity?>

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeAll(userId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE userId = :userId AND id = :id LIMIT 1")
    fun observe(userId: String, id: String): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun find(userId: String, id: String): ItemEntity?

    @Query("SELECT coverFile FROM items WHERE userId = :userId AND coverFile IS NOT NULL")
    suspend fun coverFiles(userId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ItemEntity)

    @Query("DELETE FROM items WHERE userId = :userId AND id = :id")
    suspend fun delete(userId: String, id: String)
}
