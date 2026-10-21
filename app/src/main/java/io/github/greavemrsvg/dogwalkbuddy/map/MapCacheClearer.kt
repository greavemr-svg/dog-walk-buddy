package io.github.greavemrsvg.dogwalkbuddy.map

/** Empties the map's tile cache, which implies the areas of routes the user viewed (ADR 0003, ADR 0004). */
fun interface MapCacheClearer {
    suspend fun clear()
}
