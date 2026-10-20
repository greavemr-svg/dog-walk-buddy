package io.github.greavemrsvg.dogwalkbuddy.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Local database (ADR 0004). Version 1 stays until the first release, and later tasks add their tables to it.
 * Destructive migration is forbidden because it would silently delete walk history.
 */
@Database(
    entities = [CachedCheckEntity::class, DogProfileEntity::class],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedCheckDao(): CachedCheckDao

    abstract fun dogProfileDao(): DogProfileDao

    companion object {
        const val VERSION = 1
        const val NAME = "dog_walk_buddy.db"
    }
}
