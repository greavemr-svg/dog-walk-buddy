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
            // Task 4: C2, C4, C13, D4 and D5.
            R.string.check_window_heading to "Best window, next 12 hours",
            R.string.check_retry to "Try again",
            R.string.check_can_still_record_lead to "You can still record a walk.",
            R.string.check_can_still_record to "Recording works without a connection and saves to your phone.",
            R.string.check_cant_check_explanation to
                "We couldn't get the weather, and there's no recent check to fall back on. Check your connection and try again.",
            R.string.check_needs_location to "The check needs your approximate location",
            R.string.check_location_body to "So we can get the weather where you are. Your approximate location is enough for this.",
            R.string.check_location_bullet_rounded to "Rounded to about 1 km before it leaves your phone",
            R.string.check_location_bullet_when to "Only sent when you open the check or tap refresh",
            R.string.check_location_bullet_where to "Sent only to the weather service, MET Norway",
            R.string.check_how_data_used to "How your data is used",
            // Task 5: C5, C6, C15, C18 and D6.
            R.string.recording_title to "Recording walk",
            R.string.recording_stop to "Stop and save walk",
            R.string.recording_started_label to "Started",
            R.string.recording_route_label to "Route so far",
            R.string.recording_route_caption to "Route so far · saved on this phone",
            R.string.recording_tip to
                "Pop your phone in your pocket. Recording carries on with the screen off, and a notification shows while it's on.",
            // Task 6: D7.
            R.string.history_week_range_span to "%1\$s – %2\$s",
            R.string.history_totals_dotted to "%1\$s · %2\$s · %3\$s mi",
            R.string.history_walk_at to "at %1\$s",
            R.string.history_row_spoken to "%1\$s at %2\$s, %3\$s, %4\$s mi",
            R.string.history_summary_spoken to "This week: %1\$s, %2\$s, %3\$s miles",
            R.string.history_caption_hmm to "h:mm",
            R.string.history_caption_miles to "miles",
            // Task 7: C14, C17, D8 and D10.
            R.string.walk_detail_time_range to "%1\$s – %2\$s",
            R.string.walk_detail_map_unavailable_sub to "Your route is saved. It'll show here next time you're online.",
            R.string.walk_detail_duration_caption to "Duration",
            R.string.walk_detail_distance_caption to "Distance",
            R.string.walk_detail_distance_value to "%1\$s mi",
            R.string.walk_detail_delete to "Delete this walk",
            R.string.walk_detail_back to "Back",
            R.string.privacy_title to "Privacy & your data",
            R.string.privacy_headline to "Your walks stay on your phone",
            R.string.privacy_card_local_heading to "Never leaves this phone",
            R.string.privacy_card_sent_heading to "Only sent when you use a feature",
            R.string.privacy_entry_met_heading to "MET Norway · when the check runs",
            R.string.privacy_entry_map_heading to "OpenFreeMap · when you view a walk map",
            // Task 8: C8, C9, C10, C11, D9 and D11.
            R.string.profile_title to "Your dog",
            R.string.profile_save to "Save",
            R.string.profile_replace_photo to "Change photo",
            R.string.profile_sensitivity_hint to
                "Sensitive makes the heat and hot-pavement warnings start 4°C sooner. Rain, wind and cold stay the same.",
            R.string.profile_band_normal to "Caution from 20°C / Avoid from 25°C",
            R.string.profile_band_sensitive to "Caution from 16°C / Avoid from 21°C",
            R.string.about_headline to "How we rate the weather",
            R.string.about_intro to "The overall rating is the worst of these. Heat-sensitive dogs use the values in brackets.",
            R.string.about_sources_heading to "Where this comes from",
            R.string.about_table_factor to "Factor",
            R.string.about_table_heat to "Heat",
            R.string.about_table_pavement to "Pavement",
            R.string.about_table_rain to "Rain",
            R.string.about_table_wind to "Wind",
            R.string.about_table_cold to "Cold",
            R.string.about_table_temp_go to "below 20 °C",
            R.string.about_table_temp_go_sensitive to "(16 °C)",
            R.string.about_table_temp_caution to "20 to below 25 °C",
            R.string.about_table_temp_caution_sensitive to "(16 to below 21 °C)",
            R.string.about_table_temp_avoid to "25 °C or above",
            R.string.about_table_temp_avoid_sensitive to "(21 °C or above)",
            R.string.about_table_rain_go to "below 0.5",
            R.string.about_table_rain_caution to "0.5 to 4",
            R.string.about_table_rain_avoid to "above 4",
            R.string.about_table_wind_go to "below 25",
            R.string.about_table_wind_caution to "25 to below 39",
            R.string.about_table_wind_avoid to "39 or above",
            R.string.about_table_cold_go to "above 0 °C",
            R.string.about_table_cold_caution to "0 °C or below",
            R.string.about_table_cold_avoid to "never",
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

    @Test
    fun UX_AC63_the_task_5_duration_words_read_exactly_as_approved_in_the_singular_and_plural() {
        assertEquals("1 hour", resources.getQuantityString(R.plurals.duration_hours, 1, 1))
        assertEquals("2 hours", resources.getQuantityString(R.plurals.duration_hours, 2, 2))
        assertEquals("1 minute", resources.getQuantityString(R.plurals.duration_minutes, 1, 1))
        assertEquals("23 minutes", resources.getQuantityString(R.plurals.duration_minutes, 23, 23))
        assertEquals("1 second", resources.getQuantityString(R.plurals.duration_seconds, 1, 1))
        assertEquals("0 seconds", resources.getQuantityString(R.plurals.duration_seconds, 0, 0))
        assertEquals("41 seconds", resources.getQuantityString(R.plurals.duration_seconds, 41, 41))
    }

    @Test
    fun UX_AC63_the_task_6_walks_texts_read_exactly_as_approved_when_filled_in() {
        assertEquals("Mon 19 – Sun 25 Oct", resources.getString(R.string.history_week_range_span, "Mon 19", "Sun 25 Oct"))
        val count = resources.getQuantityString(R.plurals.history_walk_count, 5, 5)
        assertEquals("5 walks · 3:10 · 9.6 mi", resources.getString(R.string.history_totals_dotted, count, "3:10", "9.6"))
        assertEquals("at 07:42", resources.getString(R.string.history_walk_at, "07:42"))
        assertEquals(
            "Wed 21 Oct at 07:42, 0:24, 1.2 mi",
            resources.getString(R.string.history_row_spoken, "Wed 21 Oct", "07:42", "0:24", "1.2"),
        )
        assertEquals(
            "This week: 3 walks, 1 hour 25 minutes, 4.2 miles",
            resources.getString(R.string.history_summary_spoken, "3 walks", "1 hour 25 minutes", "4.2"),
        )
        assertEquals("walk", resources.getQuantityString(R.plurals.history_caption_walks, 1))
        assertEquals("walks", resources.getQuantityString(R.plurals.history_caption_walks, 0))
        assertEquals("walks", resources.getQuantityString(R.plurals.history_caption_walks, 3))
    }

    @Test
    fun UX_AC63_the_task_8_counter_reads_exactly_as_approved_when_filled_in() {
        assertEquals("7 / 30", resources.getString(R.string.profile_counter, 7, 30))
        assertEquals("8 / 50", resources.getString(R.string.profile_counter, 8, 50))
    }

    @Test
    fun UX_AC33_the_notification_title_stays_recording_your_walk() {
        assertEquals("Recording your walk", resources.getString(R.string.recording_notification_title))
    }
}
