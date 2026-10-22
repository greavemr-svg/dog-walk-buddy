package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import io.github.greavemrsvg.dogwalkbuddy.data.repo.WalkRepository
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import io.github.greavemrsvg.dogwalkbuddy.service.RecordingSession
import io.github.greavemrsvg.dogwalkbuddy.service.RecordingStateHolder
import io.github.greavemrsvg.dogwalkbuddy.service.WalkRecordingController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The recording screen's state: the live session, its route so far, and whether Stop has been tapped. */
class RecordingViewModel(
    private val controller: WalkRecordingController,
    holder: RecordingStateHolder,
    walkRepository: WalkRepository,
) : ViewModel() {
    /** The walk being recorded, or null when none is (the screen then closes). */
    val session: StateFlow<RecordingSession?> = holder.state

    /**
     * The stored fixes of the walk being recorded, in order (AC35). Room emits again after each stored fix, so the
     * line updates as often as the distance. It is only queried while the screen collects it.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val route: StateFlow<List<LatLon>> =
        holder.state
            .map { it?.walkId }
            .distinctUntilChanged()
            .flatMapLatest { walkId -> walkId?.let(walkRepository::observeRoutePoints) ?: flowOf(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val mutableStopping = MutableStateFlow(false)

    /** True after Stop was tapped, until the service has saved the walk. */
    val stopping: StateFlow<Boolean> = mutableStopping.asStateFlow()

    fun stop() {
        mutableStopping.value = true
        controller.stop()
    }

    companion object {
        /** Keeps the route query alive across a short interruption such as a rotation. */
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(container: AppContainer) =
            viewModelFactory {
                initializer {
                    RecordingViewModel(
                        container.walkRecordingController,
                        container.recordingStateHolder,
                        container.walkRepository,
                    )
                }
            }
    }
}
