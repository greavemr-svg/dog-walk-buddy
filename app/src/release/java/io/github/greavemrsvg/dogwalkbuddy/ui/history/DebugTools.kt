package io.github.greavemrsvg.dogwalkbuddy.ui.history

import android.content.Context
import io.github.greavemrsvg.dogwalkbuddy.AppContainer

/** The release build has no debug actions. */
@Suppress("UNUSED_PARAMETER")
object DebugTools {
    fun historyActions(
        context: Context,
        container: AppContainer,
    ): List<DebugAction> = emptyList()
}
