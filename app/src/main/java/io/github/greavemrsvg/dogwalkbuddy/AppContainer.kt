package io.github.greavemrsvg.dogwalkbuddy

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import androidx.room.Room
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.data.location.ApproximateLocationSource
import io.github.greavemrsvg.dogwalkbuddy.data.location.LocationManagerApproximateSource
import io.github.greavemrsvg.dogwalkbuddy.data.repo.CheckRepository
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoStore
import io.github.greavemrsvg.dogwalkbuddy.data.repo.ProfileRepository
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
    val hasCoarseLocationPermission: () -> Boolean,
    val is24HourFormat: () -> Boolean,
    val filesDir: File,
) {
    val checkRepository: CheckRepository by lazy {
        CheckRepository(MetNorwayClient(weatherBaseUrl, weatherHttpClient), database.cachedCheckDao(), clock)
    }

    val photoStore: PhotoStore by lazy { PhotoStore(filesDir) }

    val profileRepository: ProfileRepository by lazy { ProfileRepository(database.dogProfileDao(), photoStore) }

    companion object {
        fun createDefault(context: Context): AppContainer {
            val appContext = context.applicationContext
            val clock = Clock.systemUTC()
            val userAgent = WeatherConfig.userAgent(BuildConfig.VERSION_NAME)
            val httpClient =
                MetNorwayClient.createHttpClient(File(appContext.cacheDir, WeatherConfig.HTTP_CACHE_DIRECTORY), userAgent)
            val database = Room.databaseBuilder(appContext, AppDatabase::class.java, AppDatabase.NAME).build()
            val hasCoarsePermission = {
                ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
            }
            val locationSource =
                LocationManagerApproximateSource(
                    locationManager = appContext.getSystemService(LocationManager::class.java),
                    hasCoarsePermission = hasCoarsePermission,
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
                hasCoarseLocationPermission = hasCoarsePermission,
                is24HourFormat = { DateFormat.is24HourFormat(appContext) },
                filesDir = appContext.filesDir,
            )
        }
    }
}
