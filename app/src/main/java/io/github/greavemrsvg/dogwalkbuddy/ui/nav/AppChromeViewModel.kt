package io.github.greavemrsvg.dogwalkbuddy.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.DogProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * State for the app shell around the screens: the saved dog name for the third nav label (AC9, AC15). It is null
 * with no profile, until the profile has loaded, and if the profile cannot be read, so the label then reads "Dog".
 */
class AppChromeViewModel(
    profile: Flow<DogProfile?>,
) : ViewModel() {
    val dogName: StateFlow<String?> =
        profile
            .map { it?.name }
            .catch { emit(null) }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(container: AppContainer) =
            viewModelFactory {
                initializer { AppChromeViewModel(container.profileRepository.observeProfile()) }
            }
    }
}
