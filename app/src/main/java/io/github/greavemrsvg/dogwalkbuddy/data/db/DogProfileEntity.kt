package io.github.greavemrsvg.dogwalkbuddy.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** The one dog profile (ADR 0004). A single row, so no row means no profile and Normal heat sensitivity is used. */
@Entity(tableName = DogProfileEntity.TABLE_NAME)
data class DogProfileEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val name: String,
    val breed: String?,
    @ColumnInfo(name = "photo_file_name") val photoFileName: String?,
    @ColumnInfo(name = "heat_sensitivity", defaultValue = "'NORMAL'") val heatSensitivity: String,
) {
    companion object {
        const val TABLE_NAME = "dog_profile"
        const val SINGLE_ROW_ID = 1
    }
}
