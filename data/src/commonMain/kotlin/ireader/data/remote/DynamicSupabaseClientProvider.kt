package ireader.data.remote

import ireader.domain.config.PlatformConfig
import ireader.domain.data.repository.SupabaseClientProvider
import ireader.domain.models.remote.SupabaseEndpoint
import ireader.domain.preferences.prefs.SupabasePreferences

/**
 * Dynamic Supabase client provider that checks configuration at access time.
 * This allows users to add/change Supabase credentials in settings and have
 * them take effect immediately without restarting the app.
 *
 * The wrapped provider is recreated only when configuration changes, avoiding
 * unnecessary object creation while maintaining dynamic config support.
 */
fun SupabaseClientProvider.unwrapDynamic(): SupabaseClientProvider =
    if (this is DynamicSupabaseClientProvider) getWrappedProvider() else this

class DynamicSupabaseClientProvider(
    private val prefs: SupabasePreferences
) : SupabaseClientProvider {

    private var cachedProvider: SupabaseClientProvider? = null
    private var lastConfigHash: Int = 0

    fun getWrappedProvider(): SupabaseClientProvider {
        ensureProviderUpdated()
        return cachedProvider!!
    }

    override fun getClient(endpoint: SupabaseEndpoint): Any {
        ensureProviderUpdated()
        return cachedProvider!!.getClient(endpoint)
    }

    override fun isEndpointAvailable(endpoint: SupabaseEndpoint): Boolean {
        ensureProviderUpdated()
        return cachedProvider!!.isEndpointAvailable(endpoint)
    }

    override fun getSupabaseUrl(): String {
        ensureProviderUpdated()
        return cachedProvider!!.getSupabaseUrl()
    }

    private fun ensureProviderUpdated() {
        val currentHash = computeConfigHash()
        if (currentHash != lastConfigHash) {
            cachedProvider = createProvider()
            lastConfigHash = currentHash
        }
    }

    private fun computeConfigHash(): Int {
        val useCustom = prefs.useCustomSupabase().get()
        val authUrl = prefs.supabaseAuthUrl().get()
        val authKey = prefs.supabaseAuthKey().get()
        return (useCustom.toString() + authUrl + authKey).hashCode()
    }

    private fun createProvider(): SupabaseClientProvider {
        val useCustom = prefs.useCustomSupabase().get()

        fun getUrl(userPref: String, platformConfig: () -> String): String {
            return if (useCustom && userPref.isNotEmpty()) {
                userPref
            } else {
                try {
                    platformConfig()
                } catch (e: Exception) {
                    ""
                }
            }
        }

        fun getKey(userPref: String, platformConfig: () -> String): String {
            return if (useCustom && userPref.isNotEmpty()) {
                userPref
            } else {
                try {
                    platformConfig()
                } catch (e: Exception) {
                    ""
                }
            }
        }

        // Load credentials with fallback chain: user preferences -> platform config
        val authUrl = getUrl(
            prefs.supabaseAuthUrl().get(),
            { PlatformConfig.getSupabaseAuthUrl() }
        )
        val authKey = getKey(
            prefs.supabaseAuthKey().get(),
            { PlatformConfig.getSupabaseAuthKey() }
        )

        val readingUrl = getUrl(
            prefs.supabaseReadingUrl().get(),
            { PlatformConfig.getSupabaseReadingUrl() }
        )
        val readingKey = getKey(
            prefs.supabaseReadingKey().get(),
            { PlatformConfig.getSupabaseReadingKey() }
        )

        val libraryUrl = getUrl(
            prefs.supabaseLibraryUrl().get(),
            { PlatformConfig.getSupabaseLibraryUrl() }
        )
        val libraryKey = getKey(
            prefs.supabaseLibraryKey().get(),
            { PlatformConfig.getSupabaseLibraryKey() }
        )

        val bookReviewsUrl = getUrl(
            prefs.supabaseBookReviewsUrl().get(),
            { PlatformConfig.getSupabaseBookReviewsUrl() }
        )
        val bookReviewsKey = getKey(
            prefs.supabaseBookReviewsKey().get(),
            { PlatformConfig.getSupabaseBookReviewsKey() }
        )

        val chapterReviewsUrl = getUrl(
            prefs.supabaseChapterReviewsUrl().get(),
            { PlatformConfig.getSupabaseChapterReviewsUrl() }
        )
        val chapterReviewsKey = getKey(
            prefs.supabaseChapterReviewsKey().get(),
            { PlatformConfig.getSupabaseChapterReviewsKey() }
        )

        val badgesUrl = getUrl(
            prefs.supabaseBadgesUrl().get(),
            { PlatformConfig.getSupabaseBadgesUrl() }
        )
        val badgesKey = getKey(
            prefs.supabaseBadgesKey().get(),
            { PlatformConfig.getSupabaseBadgesKey() }
        )

        val analyticsUrl = getUrl(
            prefs.supabaseAnalyticsUrl().get(),
            { PlatformConfig.getSupabaseAnalyticsUrl() }
        )
        val analyticsKey = getKey(
            prefs.supabaseAnalyticsKey().get(),
            { PlatformConfig.getSupabaseAnalyticsKey() }
        )

        if (authUrl.isEmpty() || authKey.isEmpty()) {
            return NoOpSupabaseClientProvider()
        }

        return MultiSupabaseClientProvider(
            authUrl = authUrl,
            authKey = authKey,
            readingUrl = readingUrl,
            readingKey = readingKey,
            libraryUrl = libraryUrl,
            libraryKey = libraryKey,
            bookReviewsUrl = bookReviewsUrl,
            bookReviewsKey = bookReviewsKey,
            chapterReviewsUrl = chapterReviewsUrl,
            chapterReviewsKey = chapterReviewsKey,
            badgesUrl = badgesUrl,
            badgesKey = badgesKey,
            analyticsUrl = analyticsUrl,
            analyticsKey = analyticsKey
        )
    }
}
