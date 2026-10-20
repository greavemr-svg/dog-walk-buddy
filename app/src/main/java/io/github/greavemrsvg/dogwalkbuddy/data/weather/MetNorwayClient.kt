package io.github.greavemrsvg.dogwalkbuddy.data.weather

import android.util.Log
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourlyConditions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The weather request failed: offline, timeout, error status, oversize or malformed body. */
class WeatherFetchException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Calls MET Norway Locationforecast 2.0 "complete" (ADR 0002). One call per [fetch], made only when the caller
 * asks, so nothing in here polls or runs in the background (AC24). Messages never carry coordinates or bodies.
 */
class MetNorwayClient(
    private val baseUrl: HttpUrl,
    private val httpClient: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val logStatus: (Int) -> Unit = { status -> Log.println(Log.WARN, LOG_TAG, "MET returned status $status") },
) {
    suspend fun fetch(
        latitude: Double,
        longitude: Double,
    ): List<HourlyConditions> {
        val url = buildUrl(latitude, longitude)
        val body = withContext(ioDispatcher) { download(url) }
        return try {
            MetResponseParser.parse(body)
        } catch (error: MalformedWeatherResponseException) {
            throw WeatherFetchException("Weather response was malformed", error)
        }
    }

    private fun buildUrl(
        latitude: Double,
        longitude: Double,
    ): HttpUrl {
        if (!latitude.isFinite() || !longitude.isFinite()) throw WeatherFetchException("Location is not finite")
        return baseUrl
            .newBuilder()
            .addPathSegment(WeatherConfig.ENDPOINT_COMPLETE)
            .addQueryParameter(WeatherConfig.QUERY_LAT, CoordinateRounding.format(latitude))
            .addQueryParameter(WeatherConfig.QUERY_LON, CoordinateRounding.format(longitude))
            .build()
    }

    /** Runs the call without blocking a thread; cancelling the coroutine cancels the OkHttp call. */
    private suspend fun download(url: HttpUrl): String =
        suspendCancellableCoroutine { continuation ->
            val call =
                httpClient.newCall(
                    Request
                        .Builder()
                        .url(url)
                        .get()
                        .build(),
                )
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        continuation.resumeWithException(WeatherFetchException("Weather request failed: ${e.javaClass.simpleName}", e))
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        try {
                            continuation.resume(readBody(response))
                        } catch (error: WeatherFetchException) {
                            continuation.resumeWithException(error)
                        } catch (error: IOException) {
                            continuation.resumeWithException(
                                WeatherFetchException("Weather request failed: ${error.javaClass.simpleName}", error),
                            )
                        }
                    }
                },
            )
        }

    private fun readBody(response: Response): String =
        response.use {
            if (it.code != WeatherConfig.HTTP_OK && it.code != WeatherConfig.HTTP_NON_AUTHORITATIVE) {
                throw WeatherFetchException("Weather request failed with status ${it.code}")
            }
            // MET asks clients to notice 203 (deprecated product). Log the status code only, nothing personal.
            if (it.code == WeatherConfig.HTTP_NON_AUTHORITATIVE) logStatus(it.code)
            val source = it.body.source()
            // Asking for one byte more than the cap tells us the body is too large without reading it all.
            if (source.request(WeatherConfig.MAX_RESPONSE_BYTES + 1)) {
                throw WeatherFetchException("Weather response is too large")
            }
            source.readUtf8()
        }

    companion object {
        private const val LOG_TAG = "MetNorwayClient"

        /**
         * The weather HTTP client: HTTPS only, identifying User-Agent, 10 s call timeout and a disk cache that
         * honours Expires and Last-Modified (ADR 0002). [configure] lets tests trust a stub server certificate.
         */
        fun createHttpClient(
            cacheDirectory: File,
            userAgent: String,
            configure: OkHttpClient.Builder.() -> Unit = {},
        ): OkHttpClient =
            OkHttpClient
                .Builder()
                .cache(Cache(cacheDirectory, WeatherConfig.HTTP_CACHE_BYTES))
                .callTimeout(WeatherConfig.callTimeout)
                // HTTPS only on every hop: never follow an https to http redirect, and check each network request.
                .followSslRedirects(false)
                .addInterceptor(HttpsOnlyInterceptor())
                .addNetworkInterceptor(HttpsOnlyInterceptor())
                .addInterceptor(UserAgentInterceptor(userAgent))
                .apply(configure)
                .build()
    }
}
