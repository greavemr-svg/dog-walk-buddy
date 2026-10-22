package io.github.greavemrsvg.dogwalkbuddy.ui.privacy

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrivacyUiState(
    /** The confirmation dialog is open. */
    val confirming: Boolean = false,
    val deleting: Boolean = false,
    /** Everything was deleted. */
    val deleted: Boolean = false,
    val deleteFailed: Boolean = false,
)

/** Holds the "Delete all my data" flow: ask, confirm, delete (AC47). */
class PrivacyViewModel(
    private val deleteAll: suspend () -> Unit,
    // The deletion must finish even if the user leaves the screen, so it runs in the application's scope.
    private val deletionScope: CoroutineScope,
    // Only the class name is logged: no message, no paths, no coordinates.
    private val logError: (String) -> Unit = { name -> Log.println(Log.WARN, LOG_TAG, "Delete all failed: $name") },
) : ViewModel() {
    private val mutableState = MutableStateFlow(PrivacyUiState())
    val state: StateFlow<PrivacyUiState> = mutableState.asStateFlow()

    fun requestDelete() {
        mutableState.update { if (it.deleting) it else it.copy(confirming = true, deleted = false, deleteFailed = false) }
    }

    /** Cancel leaves everything as it was. */
    fun cancelDelete() {
        mutableState.update { it.copy(confirming = false) }
    }

    fun confirmDelete() {
        if (!mutableState.value.confirming || mutableState.value.deleting) return
        mutableState.value = PrivacyUiState(deleting = true)
        deletionScope.launch {
            try {
                deleteAll()
                mutableState.value = PrivacyUiState(deleted = true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logError(error.javaClass.simpleName)
                mutableState.value = PrivacyUiState(deleteFailed = true)
            }
        }
    }

    fun dismissDeleteFailure() {
        mutableState.update { it.copy(deleteFailed = false) }
    }

    companion object {
        private const val LOG_TAG = "Privacy"

        fun factory(container: AppContainer) =
            viewModelFactory {
                initializer { PrivacyViewModel({ container.deleteAllDataUseCase() }, container.applicationScope) }
            }
    }
}
