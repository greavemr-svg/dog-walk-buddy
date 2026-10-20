package io.github.greavemrsvg.dogwalkbuddy.data.weather

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** Defence in depth on top of the network security config: refuses any request that is not HTTPS. */
class HttpsOnlyInterceptor(
    private val allowInsecureForTests: Boolean = false,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.url.isHttps && !allowInsecureForTests) {
            throw IOException("Refusing non-HTTPS request")
        }
        return chain.proceed(request)
    }
}
