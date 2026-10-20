package io.github.greavemrsvg.dogwalkbuddy.domain.check

import java.time.Duration
import java.time.Instant

/** When a previous check may stand in for a failed one (AC21, AC22). */
object CachePolicy {
    /** A cached check this old or older is never shown. */
    val maxAge: Duration = Duration.ofHours(3)

    fun age(
        fetchedAt: Instant,
        now: Instant,
    ): Duration = Duration.between(fetchedAt, now)

    /** True when the check is under [maxAge] old. A check dated in the future (clock moved back) is not trusted. */
    fun isUsable(
        fetchedAt: Instant,
        now: Instant,
    ): Boolean {
        val age = age(fetchedAt, now)
        return !age.isNegative && age < maxAge
    }
}
