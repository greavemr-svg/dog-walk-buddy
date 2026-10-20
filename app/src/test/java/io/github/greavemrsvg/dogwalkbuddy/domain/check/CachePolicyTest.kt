package io.github.greavemrsvg.dogwalkbuddy.domain.check

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class CachePolicyTest {
    private val fetchedAt = Instant.parse("2026-10-19T09:00:00Z")

    @Test
    fun AC21_a_check_2h59m_old_is_usable() {
        assertTrue(CachePolicy.isUsable(fetchedAt, fetchedAt.plus(Duration.ofMinutes(179))))
    }

    @Test
    fun AC21_a_check_2h59m59s_old_is_usable() {
        assertTrue(CachePolicy.isUsable(fetchedAt, fetchedAt.plusSeconds(3 * 3600L - 1)))
    }

    @Test
    fun AC22_a_check_exactly_3h_old_is_not_usable() {
        assertFalse(CachePolicy.isUsable(fetchedAt, fetchedAt.plus(Duration.ofHours(3))))
    }

    @Test
    fun AC22_a_check_older_than_3h_is_not_usable() {
        assertFalse(CachePolicy.isUsable(fetchedAt, fetchedAt.plus(Duration.ofHours(5))))
    }

    @Test
    fun AC22_a_check_dated_in_the_future_is_not_usable() {
        assertFalse(CachePolicy.isUsable(fetchedAt, fetchedAt.minusSeconds(1)))
    }

    @Test
    fun AC21_age_is_the_time_between_fetch_and_now() {
        assertEquals(Duration.ofMinutes(179), CachePolicy.age(fetchedAt, fetchedAt.plus(Duration.ofMinutes(179))))
    }
}
