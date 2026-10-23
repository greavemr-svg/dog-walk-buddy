package io.github.greavemrsvg.dogwalkbuddy.ui.copy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every AC-fixed text (spec conventions) is still exactly as the MVP criteria give it (AC62). Format strings are
 * read unformatted, so their placeholders are checked too, including the hyphen best-window range (C3 rejected).
 */
@RunWith(AndroidJUnit4::class)
class FixedTextsTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    private fun assertTexts(expected: Map<Int, String>) {
        expected.forEach { (resId, text) ->
            assertEquals(resources.getResourceEntryName(resId), text, resources.getString(resId))
        }
    }

    @Test
    fun UX_AC62_the_rating_words_are_unchanged() =
        assertTexts(
            mapOf(
                R.string.rating_go to "Go",
                R.string.rating_caution to "Caution",
                R.string.rating_avoid to "Avoid",
            ),
        )

    @Test
    fun UX_AC62_the_reason_texts_are_unchanged() =
        assertTexts(
            mapOf(
                R.string.reason_good to "Good conditions for a walk",
                R.string.reason_warm to "Warm: keep it short and bring water",
                R.string.reason_too_hot to "Too hot to walk safely",
                R.string.reason_pavement_warm to "Pavement may be hot: test it with the back of your hand",
                R.string.reason_pavement_too_hot to "Pavement likely too hot for paws",
                R.string.reason_rain to "Rain expected",
                R.string.reason_heavy_rain to "Heavy rain",
                R.string.reason_windy to "Windy",
                R.string.reason_dangerous_winds to "Dangerous winds",
                R.string.reason_freezing to "Freezing: watch for ice and grit",
                R.string.reason_data_missing to "Some weather data isn't available",
            ),
        )

    @Test
    fun UX_AC62_the_best_window_and_check_age_texts_are_unchanged() =
        assertTexts(
            mapOf(
                R.string.best_window_now_until to "Now until %1\$s",
                R.string.best_window_range to "%1\$s-%2\$s",
                R.string.best_window_best_available_caution to "Best available: Caution",
                R.string.best_window_none to "No good time in the next 12 hours",
                R.string.check_age_minutes to "Checked %1\$d min ago",
                R.string.check_age_hours_minutes to "Checked %1\$d h %2\$d min ago",
            ),
        )

    @Test
    fun UX_AC62_the_disclaimer_attribution_pollen_and_sensitivity_texts_are_unchanged() =
        assertTexts(
            mapOf(
                R.string.check_disclaimer to
                    "Guidance only, not veterinary advice. If you're worried about your dog, contact a vet.",
                R.string.home_about to "About this advice",
                R.string.about_title to "About this advice",
                R.string.check_attribution to "Weather data: MET Norway (CC BY 4.0)",
                R.string.check_pollen_not_included to "Pollen isn't included in this check",
                R.string.check_default_sensitivity to
                    "Using normal heat sensitivity. Set up your dog's profile to change this.",
            ),
        )

    @Test
    fun UX_AC62_the_empty_unavailable_map_and_delete_texts_are_unchanged() =
        assertTexts(
            mapOf(
                R.string.history_empty to "No walks yet",
                R.string.check_cant_check to "Can't check right now",
                R.string.walk_detail_map_unavailable to "Map not available offline",
                R.string.map_attribution to "© OpenStreetMap contributors · © OpenMapTiles",
                R.string.privacy_delete_all to "Delete all my data",
            ),
        )
}
