package io.github.greavemrsvg.dogwalkbuddy.testutil

import io.github.greavemrsvg.dogwalkbuddy.data.weather.MetNorwayClient
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.io.File

/** An HTTPS stub of MET Norway. The client built here trusts only this server's test certificate. */
class StubWeatherServer(
    cacheDirectory: File,
    configureClient: OkHttpClient.Builder.() -> Unit = {},
) {
    val server = MockWebServer()
    val baseUrl: HttpUrl
    val httpClient: OkHttpClient

    init {
        val localhost = HeldCertificate.Builder().addSubjectAlternativeName(HOST).build()
        val serverCertificates = HandshakeCertificates.Builder().heldCertificate(localhost).build()
        val clientCertificates = HandshakeCertificates.Builder().addTrustedCertificate(localhost.certificate).build()
        server.useHttps(serverCertificates.sslSocketFactory())
        server.start()
        baseUrl = server.url(BASE_PATH)
        httpClient =
            MetNorwayClient.createHttpClient(cacheDirectory, USER_AGENT) {
                sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
                configureClient()
            }
    }

    fun enqueueFixture(): StubWeatherServer {
        server.enqueue(
            MockResponse
                .Builder()
                .code(STATUS_OK)
                .body(fixtureText())
                .build(),
        )
        return this
    }

    fun enqueueStatus(code: Int): StubWeatherServer {
        server.enqueue(
            MockResponse
                .Builder()
                .code(code)
                .body("{}")
                .build(),
        )
        return this
    }

    fun enqueueBody(body: String): StubWeatherServer {
        server.enqueue(
            MockResponse
                .Builder()
                .code(STATUS_OK)
                .body(body)
                .build(),
        )
        return this
    }

    fun close() {
        server.close()
    }

    companion object {
        const val USER_AGENT = "DogWalkBuddy/0.0.0-test (+https://example.invalid/contact)"
        const val FIXTURE = "met/complete_london_51.51_-0.13.json"
        private const val HOST = "localhost"
        private const val BASE_PATH = "/weatherapi/locationforecast/2.0/"
        private const val STATUS_OK = 200

        fun fixtureText(): String =
            checkNotNull(StubWeatherServer::class.java.classLoader?.getResourceAsStream(FIXTURE)) { "Missing $FIXTURE" }
                .use { it.readBytes().decodeToString() }
    }
}
