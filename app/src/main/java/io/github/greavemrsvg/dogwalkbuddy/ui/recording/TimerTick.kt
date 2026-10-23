package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import java.time.Duration
import java.time.Instant

private const val MILLIS_PER_SECOND = 1_000L

/**
 * How long to wait from [now] until the time elapsed since [start] next reaches a whole second (AC34). Waiting this
 * long, rather than a fixed second after reading the clock, keeps each tick on the second boundary, so the shown
 * elapsed time never skips or repeats a second. Always between 1 ms and 1 s, also when [now] is before [start].
 */
internal fun millisUntilNextWholeSecond(
    start: Instant,
    now: Instant,
): Long = MILLIS_PER_SECOND - Math.floorMod(Duration.between(start, now).toMillis(), MILLIS_PER_SECOND)
