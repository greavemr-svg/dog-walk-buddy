package io.github.greavemrsvg.dogwalkbuddy.data.repo

import android.graphics.Bitmap
import io.github.greavemrsvg.dogwalkbuddy.data.db.DogProfileDao
import io.github.greavemrsvg.dogwalkbuddy.data.db.DogProfileEntity
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.DogProfile
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileInput
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileValidator
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ValidationResult
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** What a save does to the stored photo. */
sealed interface PhotoChange {
    data object Keep : PhotoChange

    data object Remove : PhotoChange

    data class Replace(
        val photo: ProcessedPhoto,
    ) : PhotoChange
}

sealed interface SaveResult {
    data class Saved(
        val profile: DogProfile,
    ) : SaveResult

    data class Invalid(
        val validation: ValidationResult.Invalid,
    ) : SaveResult
}

/** Owns access to the single dog profile and its photo copy (ADR 0004). */
class ProfileRepository(
    private val dao: DogProfileDao,
    private val photoStore: PhotoStore,
) {
    fun observeProfile(): Flow<DogProfile?> = dao.observe().map { it?.toDomain() }

    /** With no profile this is [HeatSensitivity.NORMAL] (AC45). Emits only when the value changes. */
    fun observeSensitivity(): Flow<HeatSensitivity> =
        observeProfile().map { it?.heatSensitivity ?: HeatSensitivity.NORMAL }.distinctUntilChanged()

    suspend fun preparePhoto(source: PhotoSource): PhotoPreparation = photoStore.prepare(source)

    suspend fun loadPhoto(profile: DogProfile): Bitmap? = profile.photoFileName?.let { photoStore.load(it) }

    /**
     * Validates at this boundary, then stores the profile. A replaced or removed photo file is deleted only after
     * the row is committed, so a failure never leaves the profile pointing at a missing file.
     */
    suspend fun save(
        input: ProfileInput,
        sensitivity: HeatSensitivity,
        photoChange: PhotoChange,
    ): SaveResult {
        val valid =
            when (val result = ProfileValidator.validate(input)) {
                is ValidationResult.Invalid -> return SaveResult.Invalid(result)
                is ValidationResult.Valid -> result.input
            }
        val previousFileName = dao.get()?.photoFileName
        val newFileName =
            when (photoChange) {
                PhotoChange.Keep -> previousFileName
                PhotoChange.Remove -> null
                is PhotoChange.Replace -> photoStore.store(photoChange.photo)
            }
        val entity =
            DogProfileEntity(
                name = valid.name,
                breed = valid.breed,
                photoFileName = newFileName,
                heatSensitivity = sensitivity.name,
            )
        try {
            dao.save(entity)
        } catch (error: Exception) {
            if (newFileName != previousFileName) newFileName?.let { photoStore.delete(it) }
            throw error
        }
        if (previousFileName != null && previousFileName != newFileName) photoStore.delete(previousFileName)
        return SaveResult.Saved(entity.toDomain())
    }

    private fun DogProfileEntity.toDomain() =
        DogProfile(
            name = name,
            breed = breed,
            photoFileName = photoFileName,
            // An unknown stored value falls back to the default rather than failing the screen.
            heatSensitivity = HeatSensitivity.entries.firstOrNull { it.name == heatSensitivity } ?: HeatSensitivity.NORMAL,
        )
}
