package io.github.greavemrsvg.dogwalkbuddy.ui.check

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.core.app.ApplicationProvider
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.FieldKitNavBar
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.Routes
import io.github.greavemrsvg.dogwalkbuddy.ui.recording.StartWalkContent
import io.github.greavemrsvg.dogwalkbuddy.ui.recording.StartWalkUiState
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import org.robolectric.RuntimeEnvironment
import java.time.ZoneOffset

/**
 * Today as the shell shows it, for one fixed non-rated state: the scrolling check content, the pinned Start walk and
 * the bottom navigation bar. It counts the taps on each control.
 */
class TodayStateHost(
    private val composeRule: ComposeContentTestRule,
) {
    var refreshes = 0
    var grants = 0
    var settingsOpens = 0
    var privacyOpens = 0
    var aboutOpens = 0

    fun show(
        state: CheckUiState,
        night: Boolean = false,
    ) {
        if (night) RuntimeEnvironment.setQualifiers("+night")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val formatter = BestWindowFormatter(context.resources, ZoneOffset.UTC, { true })
        composeRule.setContent {
            DogWalkBuddyTheme {
                Column(Modifier.fillMaxSize()) {
                    TodayScreen(
                        checkContent = {
                            CheckCard(
                                state = state,
                                windowFormatter = formatter,
                                onRefresh = { refreshes++ },
                                onGrantPermission = { grants++ },
                                onOpenAbout = { aboutOpens++ },
                                onOpenProfile = {},
                                onOpenSettings = { settingsOpens++ },
                                onOpenPrivacy = { privacyOpens++ },
                            )
                        },
                        startWalkContent = { StartWalkContent(StartWalkUiState.Idle, {}, {}, {}) },
                        modifier = Modifier.weight(1f),
                    )
                    FieldKitNavBar(selectedRoute = Routes.HOME, dogName = null, onSelect = {})
                }
            }
        }
    }
}
