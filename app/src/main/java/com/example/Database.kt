package com.example

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "resource_states")
data class ResourceState(
    @PrimaryKey val id: String,
    val name: String,
    val status: String,
    val metrics: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "browser_sessions")
data class BrowserSession(
    @PrimaryKey val id: String,
    val name: String,
    val tabsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ResourceStateDao {
    @Query("SELECT * FROM resource_states ORDER BY timestamp DESC")
    fun getAllStates(): Flow<List<ResourceState>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertState(state: ResourceState)

    @Query("DELETE FROM resource_states WHERE id = :id")
    suspend fun deleteStateById(id: String)
}

@Dao
interface BrowserSessionDao {
    @Query("SELECT * FROM browser_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<BrowserSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: BrowserSession)

    @Query("DELETE FROM browser_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)
}

@Database(entities = [ResourceState::class, BrowserSession::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resourceStateDao(): ResourceStateDao
    abstract fun browserSessionDao(): BrowserSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "openclaw_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
