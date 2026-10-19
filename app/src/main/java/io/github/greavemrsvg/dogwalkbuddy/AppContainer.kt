package io.github.greavemrsvg.dogwalkbuddy

import java.time.Clock
import java.time.ZoneId

/**
 * Manual dependency injection (ADR 0001). Time and zone are injected so that tests are deterministic.
 * Later tasks add the other collaborators listed in the design.
 */
class AppContainer(
    val clock: Clock,
    val zoneId: ZoneId,
) {
    companion object {
        fun createDefault(): AppContainer = AppContainer(clock = Clock.systemUTC(), zoneId = ZoneId.systemDefault())
    }
}
