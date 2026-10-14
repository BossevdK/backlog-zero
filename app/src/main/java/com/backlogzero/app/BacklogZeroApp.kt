package com.backlogzero.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.backlogzero.app.data.AuthRepository
import com.backlogzero.app.data.CoverStorage
import com.backlogzero.app.data.LibraryRepository
import com.backlogzero.app.data.LocalAuthRepository
import com.backlogzero.app.data.RoomLibraryRepository
import com.backlogzero.app.data.SessionStore
import com.backlogzero.app.data.db.AppDatabase
import com.backlogzero.app.domain.Recommender
import com.backlogzero.app.domain.WeightedRandomRecommender

/** Handmatige afhankelijkheden: één plek om implementaties later te verwisselen. */
class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    val covers = CoverStorage(context)
    val auth: AuthRepository = LocalAuthRepository(database.userDao(), SessionStore(context))
    val library: LibraryRepository = RoomLibraryRepository(database.itemDao(), auth, covers)
    val recommender: Recommender = WeightedRandomRecommender()
}

class BacklogZeroApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
