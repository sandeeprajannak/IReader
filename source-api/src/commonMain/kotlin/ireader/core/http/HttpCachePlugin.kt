package ireader.core.http

import io.ktor.client.HttpClientConfig
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpResponseData
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpMethod
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import ireader.core.util.currentTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

class HttpCacheConfig {
    var cacheDurationMs: Long = 5 * 60 * 1000
    var enabled: Boolean = true
    var cacheableMethods: Set<HttpMethod> = setOf(HttpMethod.Get)
    var cacheableStatusCodes: Set<HttpStatusCode> = setOf(HttpStatusCode.OK)
    var shouldCache: (HttpRequestBuilder) -> Boolean = { true }
}

/**
 * Ktor plugin for caching HTTP responses
 */
@OptIn(InternalAPI::class)
val HttpCachePlugin = createClientPlugin("HttpCachePlugin", ::HttpCacheConfig) {
    val cache = HttpCache(pluginConfig.cacheDurationMs)
    
    // Intercept requests
    on(Send) { request ->
        // Check for per-request cache control
        val cacheControl = request.attributes.getOrNull(CacheControlAttribute)

        // Check if caching is enabled and method is cacheable
        if (!pluginConfig.enabled ||
            request.method !in pluginConfig.cacheableMethods ||
            !pluginConfig.shouldCache(request) ||
            cacheControl?.useCache == false) {
            return@on proceed(request)
        }

        // Generate cache key
        val cacheKey = cache.generateKey(request.url.toString(), request.method)

        // Try to get from cache (unless force refresh)
        if (cacheControl?.forceRefresh != true) {
            val cachedEntry = cache.get(cacheKey)
            if (cachedEntry != null) {
                // Give this synthetic call its own child Job under the client's scope, rather than
                // reusing request.executionContext (too short-lived for a redirect follow-up
                // sub-request, causing an indefinite hang) or the client's own coroutineContext
                // directly (Ktor completes/cancels a call's callContext Job when that call finishes
                // reading its response — reusing the client's Job here would tear down the client's
                // root Job on first use, killing every subsequent request through it).
                val callContext = client.coroutineContext + SupervisorJob(client.coroutineContext[Job])
                val responseData = HttpResponseData(
                    statusCode = cachedEntry.statusCode,
                    requestTime = GMTDate(),
                    headers = cachedEntry.headers,
                    version = HttpProtocolVersion.HTTP_1_1,
                    body = ByteReadChannel(cachedEntry.response),
                    callContext = callContext
                )
                return@on HttpClientCall(client, request.build(), responseData)
            }
        }

        // Proceed with actual request
        val call = proceed(request)

        // Cache response if status code is cacheable
        if (call.response.status in pluginConfig.cacheableStatusCodes) {
            try {
                val responseBody = call.response.readRawBytes()
                // Use custom cache duration if specified
                val cacheDuration = cacheControl?.cacheDurationMs ?: pluginConfig.cacheDurationMs
                val entry = CacheEntry(
                    response = responseBody,
                    contentType = call.response.contentType(),
                    headers = call.response.headers,
                    statusCode = call.response.status,
                    expiresAt = currentTimeMillis() + cacheDuration
                )
                cache.put(cacheKey, entry)
                
                // Return new call with cached body
                val newResponseData = HttpResponseData(
                    statusCode = call.response.status,
                    requestTime = call.response.responseTime,
                    headers = call.response.headers,
                    version = call.response.version,
                    body = ByteReadChannel(responseBody),
                    callContext = call.response.coroutineContext
                )
                HttpClientCall(client, request.build(), newResponseData)
            } catch (e: Exception) {
                // If caching fails, return original call
                call
            }
        } else {
            call
        }
    }
}

/**
 * Extension to install cache plugin with custom configuration
 */
fun HttpClientConfig<*>.installCache(
    cacheDurationMs: Long = 5 * 60 * 1000,
    block: HttpCacheConfig.() -> Unit = {}
) {
    install(HttpCachePlugin) {
        this.cacheDurationMs = cacheDurationMs
        block()
    }
}
