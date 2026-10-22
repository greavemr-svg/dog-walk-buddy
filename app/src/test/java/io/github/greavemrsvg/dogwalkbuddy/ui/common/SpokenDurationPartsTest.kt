package io.github.greavemrsvg.dogwalkbuddy.ui.common

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration

/** The spoken timer's parts and words (AC34, D6), including the default smallest unit and the singular forms. */
@RunWith(AndroidJUnit4::class)
class SpokenDurationPartsTest {
    private val resources: Resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources

    @Test
    fun UX_AC34_parts_default_to_seconds_and_leave_out_zero_units() {
        assertEquals(
            listOf(SpokenUnit.HOURS to 1L, SpokenUnit.MINUTES to 2L, SpokenUnit.SECONDS to 5L),
            SpokenDuration.parts(Duration.ofSeconds(3725)),
        )
        assertEquals(
            listOf(SpokenUnit.HOURS to 1L, SpokenUnit.SECONDS to 5L),
            SpokenDuration.parts(Duration.ofSeconds(3605)),
        )
        assertEquals(listOf(SpokenUnit.SECONDS to 0L), SpokenDuration.parts(Duration.ZERO))
        assertEquals(listOf(SpokenUnit.SECONDS to 0L), SpokenDuration.parts(Duration.ofMillis(-1)))
    }

    @Test
    fun UX_AC34_each_unit_on_its_own_is_singular_at_one_and_plural_otherwise() {
        assertEquals("1 second", SpokenDuration.format(resources, Duration.ofSeconds(1)))
        assertEquals("1 minute", SpokenDuration.format(resources, Duration.ofMinutes(1)))
        assertEquals("1 hour", SpokenDuration.format(resources, Duration.ofHours(1)))
        assertEquals("2 seconds", SpokenDuration.format(resources, Duration.ofSeconds(2)))
        assertEquals("2 minutes", SpokenDuration.format(resources, Duration.ofMinutes(2)))
        assertEquals("1 minute 1 second", SpokenDuration.format(resources, Duration.ofSeconds(61)))
        assertEquals("2 hours 1 minute", SpokenDuration.format(resources, Duration.ofMinutes(121)))
    }

    @Test
    fun UX_AC34_the_boundaries_around_a_minute_and_an_hour_are_spoken_exactly() {
        assertEquals("59 seconds", SpokenDuration.format(resources, Duration.ofSeconds(59)))
        assertEquals("59 minutes 59 seconds", SpokenDuration.format(resources, Duration.ofSeconds(3599)))
        assertEquals("12 hours", SpokenDuration.format(resources, Duration.ofHours(12)))
        assertEquals("25 hours 1 second", SpokenDuration.format(resources, Duration.ofHours(25).plusSeconds(1)))
    }
}
