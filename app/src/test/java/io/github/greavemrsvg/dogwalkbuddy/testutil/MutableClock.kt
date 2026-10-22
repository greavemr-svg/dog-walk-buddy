package io.github.greavemrsvg.dogwalkbuddy.testutil

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock the test moves by hand, so time-dependent rules are deterministic. */
class MutableClock(
    private var now: Instant,
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {
    fun advance(duration: Duration) {
        now = now.plus(duration)
    }

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)

    override fun instant(): Instant = now
}
