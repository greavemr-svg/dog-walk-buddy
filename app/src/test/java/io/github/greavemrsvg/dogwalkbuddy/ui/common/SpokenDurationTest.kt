package io.github.greavemrsvg.dogwalkbuddy.ui.common

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration

@RunWith(AndroidJUnit4::class)
class SpokenDurationTest {
    private val resources: Resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources

    private fun spoken(duration: Duration) = SpokenDuration.format(resources, duration)

    @Test
    fun UX_AC34_minutes_and_seconds_are_spoken_in_words() {
        assertEquals("23 minutes 41 seconds", spoken(Duration.ofMinutes(23).plusSeconds(41)))
    }

    @Test
    fun UX_AC34_hours_minutes_and_seconds_are_spoken_in_words() {
        assertEquals("1 hour 2 minutes 5 seconds", spoken(Duration.ofHours(1).plusMinutes(2).plusSeconds(5)))
    }

    @Test
    fun UX_AC34_units_whose_value_is_zero_are_left_out() {
        assertEquals("1 hour 5 seconds", spoken(Duration.ofHours(1).plusSeconds(5)))
        assertEquals("2 hours", spoken(Duration.ofHours(2)))
        assertEquals("10 minutes", spoken(Duration.ofMinutes(10)))
    }

    @Test
    fun UX_AC34_a_zero_duration_is_zero_seconds() {
        assertEquals("0 seconds", spoken(Duration.ZERO))
    }

    @Test
    fun UX_AC34_a_value_of_one_uses_the_singular() {
        assertEquals("1 hour 1 minute 1 second", spoken(Duration.ofSeconds(3661)))
    }

    @Test
    fun a_negative_duration_counts_as_zero_and_parts_below_a_second_are_floored() {
        assertEquals("0 seconds", spoken(Duration.ofSeconds(-30)))
        assertEquals("59 seconds", spoken(Duration.ofMillis(59_999)))
    }

    @Test
    fun the_smallest_unit_can_be_minutes_for_later_screens() {
        assertEquals("1 hour 2 minutes", SpokenDuration.format(resources, Duration.ofSeconds(3725), SpokenUnit.MINUTES))
        assertEquals("0 minutes", SpokenDuration.format(resources, Duration.ofSeconds(59), SpokenUnit.MINUTES))
    }
}
