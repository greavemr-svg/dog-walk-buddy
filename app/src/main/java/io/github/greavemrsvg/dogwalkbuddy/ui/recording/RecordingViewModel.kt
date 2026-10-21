package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import io.github.greavemrsvg.dogwalkbuddy.service.RecordingSession
import io.github.greavemrsvg.dogwalkbuddy.service.RecordingStateHolder
import io.github.greavemrsvg.dogwalkbuddy.service.WalkRecordingController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The recording screen's state: the live session, and whether Stop has been tapped. */
class RecordingViewModel(
    private val controller: WalkRecordingController,
    holder: RecordingStateHolder,
) : ViewModel() {
    /** The walk being recorded, or null when none is (the screen then closes). */
    val session: StateFlow<RecordingSession?> = holder.state

    private val mutableStopping = MutableStateFlow(false)

    /** True after Stop was tapped, until the service has saved the walk. */
    val stopping: StateFlow<Boolean> = mutableStopping.asStateFlow()

    fun stop() {
        mutableStopping.value = true
        controller.stop()
    }

    companion object {
        fun factory(container: AppContainer) =
            viewModelFactory {
                initializer {
                    RecordingViewModel(container.walkRecordingController, container.recordingStateHolder)
                }
            }
    }
}
