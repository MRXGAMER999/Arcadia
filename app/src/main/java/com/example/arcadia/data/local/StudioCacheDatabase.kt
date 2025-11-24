package com.example.arcadia.data.local

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
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "studio_expansions")
data class StudioExpansionEntity(
    @PrimaryKey
    val parentStudio: String,
    val subsidiaries: List<String>,
    val timestamp: Long,
    val slugs: String? = null // Comma-separated slugs for API filtering
)

class StudioListConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromList(list: List<String>): String = json.encodeToString(list)

    @TypeConverter
    fun toList(value: String): List<String> = json.decodeFromString(value)
}

@Dao
interface StudioDao {
    @Query("SELECT * FROM studio_expansions WHERE parentStudio = :parentStudio")
    suspend fun getExpansion(parentStudio: String): StudioExpansionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpansion(entity: StudioExpansionEntity)

    @Query("DELETE FROM studio_expansions WHERE timestamp < :expirationTime")
    suspend fun deleteExpired(expirationTime: Long)

    @Query("DELETE FROM studio_expansions")
    suspend fun clearAll()
}

@Database(entities = [StudioExpansionEntity::class], version = 2, exportSchema = false)
@TypeConverters(StudioListConverter::class)
abstract class StudioCacheDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: StudioCacheDatabase? = null

        fun getInstance(context: Context): StudioCacheDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudioCacheDatabase::class.java,
                    "studio_cache_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true) // OK for cache DB
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
