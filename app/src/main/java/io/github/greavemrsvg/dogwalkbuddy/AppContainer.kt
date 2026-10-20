package io.github.greavemrsvg.dogwalkbuddy

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.room.Room
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.data.location.ApproximateLocationSource
import io.github.greavemrsvg.dogwalkbuddy.data.location.LocationManagerApproximateSource
import io.github.greavemrsvg.dogwalkbuddy.data.repo.CheckRepository
import io.github.greavemrsvg.dogwalkbuddy.data.weather.MetNorwayClient
import io.github.greavemrsvg.dogwalkbuddy.data.weather.WeatherConfig
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.File
import java.time.Clock
import java.time.ZoneId

/**
 * Manual dependency injection (ADR 0001). Time, zone, the weather base URL and HTTP client, the database and the
 * location source are injected so that tests are deterministic. Nothing here makes a network request on creation,
 * so weather is requested only when the check asks for it (AC24).
 */
class AppContainer(
    val clock: Clock,
    val zoneId: ZoneId,
    val weatherBaseUrl: HttpUrl,
    val weatherHttpClient: OkHttpClient,
    val database: AppDatabase,
    val approximateLocationSource: ApproximateLocationSource,
) {
    val checkRepository: CheckRepository by lazy {
        CheckRepository(MetNorwayClient(weatherBaseUrl, weatherHttpClient), database.cachedCheckDao(), clock)
    }

    companion object {
        fun createDefault(context: Context): AppContainer {
            val appContext = context.applicationContext
            val clock = Clock.systemUTC()
            val userAgent = WeatherConfig.userAgent(BuildConfig.VERSION_NAME)
            val httpClient =
                MetNorwayClient.createHttpClient(File(appContext.cacheDir, WeatherConfig.HTTP_CACHE_DIRECTORY), userAgent)
            val database = Room.databaseBuilder(appContext, AppDatabase::class.java, AppDatabase.NAME).build()
            val locationSource =
                LocationManagerApproximateSource(
                    locationManager = appContext.getSystemService(LocationManager::class.java),
                    hasCoarsePermission = {
                        ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED
                    },
                    clock = clock,
                    executor = ContextCompat.getMainExecutor(appContext),
                )
            return AppContainer(
                clock = clock,
                zoneId = ZoneId.systemDefault(),
                weatherBaseUrl = WeatherConfig.productionBaseUrl,
                weatherHttpClient = httpClient,
                database = database,
                approximateLocationSource = locationSource,
            )
        }
    }
}
