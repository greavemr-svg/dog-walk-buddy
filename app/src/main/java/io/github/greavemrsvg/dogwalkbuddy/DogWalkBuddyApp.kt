package io.github.greavemrsvg.dogwalkbuddy

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class DogWalkBuddyApp : Application() {
    lateinit var container: AppContainer
        // Internal so that tests can install a container with fakes before a screen or the service uses it.
        internal set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.createDefault(this)
        recoverInterruptedWalk()
    }

    // A walk still marked RECORDING in a fresh process was cut short by a process kill (AC34).
    private fun recoverInterruptedWalk() {
        val current = container
        current.applicationScope.launch {
            try {
                current.walkRecordingController.recoverInterrupted()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Only the class name is logged: no message, no coordinates.
                Log.println(Log.WARN, LOG_TAG, "Walk recovery failed: ${error.javaClass.simpleName}")
            }
        }
    }

    private companion object {
        const val LOG_TAG = "DogWalkBuddy"
    }
}
