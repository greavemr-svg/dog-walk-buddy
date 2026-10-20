package io.github.greavemrsvg.dogwalkbuddy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DogProfileDao {
    @Query("SELECT * FROM dog_profile WHERE id = ${DogProfileEntity.SINGLE_ROW_ID}")
    fun observe(): Flow<DogProfileEntity?>

    @Query("SELECT * FROM dog_profile WHERE id = ${DogProfileEntity.SINGLE_ROW_ID}")
    suspend fun get(): DogProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: DogProfileEntity)
}
