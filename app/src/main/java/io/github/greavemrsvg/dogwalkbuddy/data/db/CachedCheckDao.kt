package io.github.greavemrsvg.dogwalkbuddy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedCheckDao {
    @Query("SELECT * FROM cached_check WHERE id = ${CachedCheckEntity.SINGLE_ROW_ID}")
    suspend fun get(): CachedCheckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: CachedCheckEntity)

    @Query("DELETE FROM cached_check")
    suspend fun clear()
}
