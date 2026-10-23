package io.github.greavemrsvg.dogwalkbuddy.testutil

import android.os.Looper
import org.junit.Assert.fail
import org.robolectric.Shadows.shadowOf

/**
 * Lets the main looper run, and background work (Room queries) finish, until [condition] holds. The main looper
 * is paused in Robolectric, so coroutines on the main dispatcher only run while this idles it.
 */
fun awaitCondition(
    what: String,
    timeoutMs: Long = 10_000,
    condition: () -> Boolean,
) {
    val deadline = System.nanoTime() + timeoutMs * 1_000_000
    while (!condition()) {
        shadowOf(Looper.getMainLooper()).idle()
        if (condition()) return
        if (System.nanoTime() > deadline) fail("Timed out waiting for: $what")
        Thread.sleep(5)
    }
}
