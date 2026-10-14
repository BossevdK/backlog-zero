package com.backlogzero.app

import android.app.Application
import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess
import androidx.room.Room
import com.backlogzero.app.data.AuthRepository
import com.backlogzero.app.data.CoverStorage
import com.backlogzero.app.data.LibraryRepository
import com.backlogzero.app.data.LocalAuthRepository
import com.backlogzero.app.data.MetadataSearch
import com.backlogzero.app.data.OnlineMetadataSearch
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
    val metadata: MetadataSearch = OnlineMetadataSearch()
    val recommender: Recommender = WeightedRandomRecommender()
}

class BacklogZeroApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        installCrashScreen()
        container = AppContainer(this)
    }

    /** Bij een crash de foutmelding tonen in een los scherm (eigen proces), zodat hij af te lezen is. */
    private fun installCrashScreen() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            val text = runCatching { android.util.Log.getStackTraceString(e).take(6000) }.getOrDefault(e.toString())
            val started = runCatching {
                startActivity(
                    Intent(this, CrashActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra("t", text),
                )
            }.isSuccess
            if (started) {
                runCatching { Thread.sleep(500) }
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            } else {
                previous?.uncaughtException(thread, e)
            }
        }
    }
}
