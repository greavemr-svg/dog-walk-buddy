package io.github.greavemrsvg.dogwalkbuddy.ui.profile

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoChange
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoPreparation
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoSource
import io.github.greavemrsvg.dogwalkbuddy.data.repo.ProcessedPhoto
import io.github.greavemrsvg.dogwalkbuddy.data.repo.ProfileRepository
import io.github.greavemrsvg.dogwalkbuddy.data.repo.SaveResult
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileError
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileInput
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PhotoProblem { TOO_LARGE, NOT_AN_IMAGE, UNREADABLE }

data class ProfileUiState(
    val isLoaded: Boolean = false,
    val name: String = "",
    val breed: String = "",
    val sensitivity: HeatSensitivity = HeatSensitivity.NORMAL,
    val photo: ImageBitmap? = null,
    val nameError: ProfileError? = null,
    val breedError: ProfileError? = null,
    val photoProblem: PhotoProblem? = null,
    val isSaving: Boolean = false,
    val justSaved: Boolean = false,
    val saveFailed: Boolean = false,
    /** The stored profile could not be read, so the form is empty although a profile may exist. */
    val loadFailed: Boolean = false,
    /** Counts refused saves, so the screen can move focus to the first field in error each time. */
    val refusedSaves: Int = 0,
)

/** Holds the profile form. Edits stay in this state until Save, which is the only write (AC41, AC42, AC43). */
class ProfileViewModel(
    private val repository: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = mutableState.asStateFlow()

    private var pendingPhoto: ProcessedPhoto? = null
    private var removeStoredPhoto = false
    private var hasStoredPhoto = false

    init {
        viewModelScope.launch {
            try {
                val profile = repository.observeProfile().first()
                if (profile != null) {
                    hasStoredPhoto = profile.photoFileName != null
                    val photo = repository.loadPhoto(profile)?.asImageBitmap()
                    mutableState.update {
                        it.copy(
                            name = profile.name,
                            breed = profile.breed.orEmpty(),
                            sensitivity = profile.heatSensitivity,
                            photo = photo,
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A failed load leaves an empty form the user can still fill in, and is shown as a message.
                // Nothing about the profile is logged.
                mutableState.update { it.copy(loadFailed = true) }
            } finally {
                // The form must never stay disabled because loading failed.
                mutableState.update { it.copy(isLoaded = true) }
            }
        }
    }

    // Until the stored profile has loaded, edits are ignored (the screen disables the controls), so loading
    // can never overwrite what the user typed.
    fun onNameChange(value: String) {
        if (!mutableState.value.isLoaded) return
        mutableState.update { it.copy(name = value.take(INPUT_HARD_CAP), nameError = null, justSaved = false) }
    }

    fun onBreedChange(value: String) {
        if (!mutableState.value.isLoaded) return
        mutableState.update { it.copy(breed = value.take(INPUT_HARD_CAP), breedError = null, justSaved = false) }
    }

    fun onSensitivityChange(value: HeatSensitivity) {
        if (!mutableState.value.isLoaded) return
        mutableState.update { it.copy(sensitivity = value, justSaved = false) }
    }

    fun onPhotoPicked(source: PhotoSource) {
        if (!mutableState.value.isLoaded) return
        viewModelScope.launch {
            when (val result = repository.preparePhoto(source)) {
                is PhotoPreparation.Ready -> {
                    pendingPhoto = result.photo
                    removeStoredPhoto = false
                    mutableState.update { it.copy(photo = result.photo.bitmap.asImageBitmap(), photoProblem = null, justSaved = false) }
                }
                PhotoPreparation.TooLarge -> showPhotoProblem(PhotoProblem.TOO_LARGE)
                PhotoPreparation.NotAnImage -> showPhotoProblem(PhotoProblem.NOT_AN_IMAGE)
                PhotoPreparation.Unreadable -> showPhotoProblem(PhotoProblem.UNREADABLE)
            }
        }
    }

    fun onRemovePhoto() {
        if (!mutableState.value.isLoaded) return
        pendingPhoto = null
        removeStoredPhoto = hasStoredPhoto
        mutableState.update { it.copy(photo = null, photoProblem = null, justSaved = false) }
    }

    fun onSave() {
        val current = mutableState.value
        if (current.isSaving || !current.isLoaded) return
        // The photo change is fixed now, so a photo picked while the save runs is not lost or mistaken for this one.
        val submittedPhoto = pendingPhoto
        val change =
            when {
                submittedPhoto != null -> PhotoChange.Replace(submittedPhoto)
                removeStoredPhoto -> PhotoChange.Remove
                else -> PhotoChange.Keep
            }
        mutableState.update { it.copy(isSaving = true, justSaved = false, saveFailed = false) }
        viewModelScope.launch {
            val result =
                try {
                    repository.save(ProfileInput(current.name, current.breed), current.sensitivity, change)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // Nothing about the profile is logged. The user is told and can try again.
                    mutableState.update { it.copy(isSaving = false, saveFailed = true) }
                    return@launch
                }
            when (result) {
                is SaveResult.Invalid ->
                    mutableState.update {
                        it.copy(
                            nameError = result.validation.nameError,
                            breedError = result.validation.breedError,
                            isSaving = false,
                            refusedSaves = it.refusedSaves + 1,
                        )
                    }
                is SaveResult.Saved -> {
                    hasStoredPhoto = result.profile.photoFileName != null
                    if (pendingPhoto === submittedPhoto) {
                        pendingPhoto = null
                        removeStoredPhoto = false
                    } else if (pendingPhoto == null) {
                        // The photo was removed while saving: the photo that was just stored still has to go.
                        removeStoredPhoto = hasStoredPhoto
                    }
                    mutableState.update {
                        it.copy(
                            name = result.profile.name,
                            breed = result.profile.breed.orEmpty(),
                            isSaving = false,
                            justSaved = true,
                        )
                    }
                }
            }
        }
    }

    private fun showPhotoProblem(problem: PhotoProblem) = mutableState.update { it.copy(photoProblem = problem) }

    companion object {
        // Far above the limits, so a huge paste cannot bloat the state, while the limit message still shows (AC42).
        const val INPUT_HARD_CAP = 200

        fun factory(container: AppContainer) =
            viewModelFactory {
                initializer { ProfileViewModel(container.profileRepository) }
            }
    }
}
