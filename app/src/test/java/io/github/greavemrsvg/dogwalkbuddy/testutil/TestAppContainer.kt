package io.github.greavemrsvg.dogwalkbuddy.testutil

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.data.location.ApproximateLocationSource
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import org.junit.rules.TemporaryFolder
import java.time.Instant
import java.time.ZoneOffset

/**
 * An [AppContainer] for screen tests: in-memory database, an HTTPS stub of MET Norway, a clock the test moves,
 * and a location source and permission flag the test controls. Nothing here touches the real network.
 */
class TestAppContainer(
    temporaryFolder: TemporaryFolder,
    start: Instant = DEFAULT_START,
) {
    val stub = StubWeatherServer(temporaryFolder.newFolder())
    val clock = MutableClock(start)
    var permissionGranted = true
    var location: LocationFix? = LocationFix(51.507351, -0.127758, accuracyM = 1000.0, timeUtc = start)

    val database: AppDatabase =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    val container =
        AppContainer(
            clock = clock,
            zoneId = ZoneOffset.UTC,
            weatherBaseUrl = stub.baseUrl,
            weatherHttpClient = stub.httpClient,
            database = database,
            approximateLocationSource = ApproximateLocationSource { location },
            hasCoarseLocationPermission = { permissionGranted },
            is24HourFormat = { true },
            filesDir = temporaryFolder.newFolder(),
        )

    fun close() {
        database.close()
        stub.close()
    }

    companion object {
        /** Inside the first hourly period (14:00 UTC) of the committed MET fixture. */
        val DEFAULT_START: Instant = Instant.parse("2026-10-19T14:20:00Z")
    }
}
