package io.github.greavemrsvg.dogwalkbuddy.map.maplibre

import android.content.Context
import io.github.greavemrsvg.dogwalkbuddy.map.MapCacheClearer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.maplibre.android.offline.OfflineManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Clears MapLibre's ambient tile cache for "Delete all my data" (ADR 0003). Excluded from coverage. */
class MapLibreCacheClearer(
    private val appContext: Context,
    private val httpClient: OkHttpClient,
) : MapCacheClearer {
    override suspend fun clear() {
        // MapLibre is used from the main thread and answers on it.
        withContext(Dispatchers.Main) {
            MapLibreRuntime.ensureInitialised(appContext, httpClient)
            suspendCancellableCoroutine { continuation ->
                OfflineManager.getInstance(appContext).clearAmbientCache(
                    object : OfflineManager.FileSourceCallback {
                        override fun onSuccess() = continuation.resume(Unit)

                        override fun onError(message: String) =
                            continuation.resumeWithException(IllegalStateException("Map cache not cleared"))
                    },
                )
            }
        }
    }
}
