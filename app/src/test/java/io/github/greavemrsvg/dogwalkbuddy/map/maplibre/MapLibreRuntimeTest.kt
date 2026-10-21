package io.github.greavemrsvg.dogwalkbuddy.map.maplibre

import org.junit.Assert.assertEquals
import org.junit.Test

class MapLibreRuntimeTest {
    @Test
    fun startsMapLibreBeforeSettingTheHttpClient() {
        val steps = mutableListOf<String>()

        MapLibreRuntime.start(
            startMapLibre = { steps += "getInstance" },
            setHttpClient = { steps += "setOkHttpClient" },
        )

        assertEquals(listOf("getInstance", "setOkHttpClient"), steps)
    }
}
