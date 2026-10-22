package io.github.greavemrsvg.dogwalkbuddy.ui.copy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Each new or changed text of this run reads exactly as approved (AC63). Each task adds the rows for the items it
 * applies (design, "Approved copy by task"). Task 1 adds only the two font-name headings that AC8 requires.
 */
@RunWith(AndroidJUnit4::class)
class ApprovedCopyTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    /** String resource to its exact approved text. */
    private val approved: Map<Int, String> =
        mapOf(
            R.string.about_font_bricolage_heading to "Bricolage Grotesque",
            R.string.about_font_figtree_heading to "Figtree",
            // Task 2: C7, C16 and the D9 tiles.
            R.string.history_title to "Walks",
            R.string.nav_today to "Today",
            R.string.nav_walks to "Walks",
            R.string.nav_dog to "Dog",
            R.string.profile_privacy_tile to "Privacy & data",
            R.string.profile_about_tile to "About the advice",
            // Task 3: C1, C12, D1, D2 and D3.
            R.string.check_title_named to "Good time to walk %1\$s?",
            R.string.check_banner_offline_lead to "You're offline.",
            R.string.check_banner_cached to "Showing your last check. It's kept for up to 3 hours.",
            R.string.check_meta_just_now to "Your area · checked just now",
            R.string.check_sensitive_thresholds to "Heat-sensitive thresholds (%1\$s)",
            R.string.check_factor_air to "Air",
            R.string.check_factor_rain to "Rain",
            R.string.check_factor_wind to "Wind",
            R.string.check_factor_pavement to "Pavement",
            R.string.check_factor_temperature_value to "%1\$d°C",
            R.string.check_factor_rain_value to "%1\$d mm/h",
            R.string.check_factor_wind_value to "%1\$d mph",
            R.string.check_factor_not_rated to "Not rated",
            R.string.check_factor_no_data to "No data",
            R.string.check_pavement_cool to "Cool",
            R.string.check_pavement_warm to "Warm",
            R.string.check_pavement_hot to "Hot",
            R.string.check_factor_spoken to "%1\$s %2\$s",
        )

    @Test
    fun UX_AC63_the_task_3_texts_read_exactly_as_approved_when_filled_in() {
        assertEquals("Good time to walk Biscuit?", resources.getString(R.string.check_title_named, "Biscuit"))
        assertEquals("Heat-sensitive thresholds (Biscuit)", resources.getString(R.string.check_sensitive_thresholds, "Biscuit"))
        assertEquals("-3°C", resources.getString(R.string.check_factor_temperature_value, -3L))
        assertEquals("0 mm/h", resources.getString(R.string.check_factor_rain_value, 0L))
        assertEquals("9 mph", resources.getString(R.string.check_factor_wind_value, 9L))
    }

    @Test
    fun UX_AC63_each_approved_text_reads_exactly_as_approved() {
        approved.forEach { (resId, text) ->
            assertEquals(resources.getResourceEntryName(resId), text, resources.getString(resId))
        }
    }
}
