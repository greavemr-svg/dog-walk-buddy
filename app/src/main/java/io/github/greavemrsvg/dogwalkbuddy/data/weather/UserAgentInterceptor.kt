package io.github.greavemrsvg.dogwalkbuddy.data.weather

import okhttp3.Interceptor
import okhttp3.Response

/** Sets the identifying User-Agent, the only header that identifies the app to MET Norway. */
class UserAgentInterceptor(
    private val userAgent: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain
                .request()
                .newBuilder()
                .header(USER_AGENT_HEADER, userAgent)
                .build(),
        )

    private companion object {
        const val USER_AGENT_HEADER = "User-Agent"
    }
}
