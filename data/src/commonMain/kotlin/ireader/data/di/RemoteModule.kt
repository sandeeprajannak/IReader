package ireader.data.di

import io.github.jan.supabase.SupabaseClient
import ireader.data.remote.DynamicSupabaseClientProvider
import ireader.data.remote.MultiSupabaseClientProvider
import ireader.data.remote.RemoteCache
import ireader.data.remote.RetryPolicy
import ireader.data.remote.SupabaseRemoteRepository
import ireader.data.remote.SyncQueue
import ireader.domain.data.repository.RemoteRepository
import ireader.domain.data.repository.SupabaseClientProvider
import ireader.domain.models.remote.SupabaseEndpoint
import ireader.domain.preferences.prefs.SupabasePreferences
import ireader.data.backend.AuthUser
import kotlinx.serialization.json.JsonElement
import org.koin.dsl.module

/**
 * Remote module for 7-project Supabase setup
 *
 * This module provides the DynamicSupabaseClientProvider that checks configuration
 * at access time (not creation time), allowing users to add/change Supabase config
 * in settings and have it take effect immediately without restarting the app.
 *
 * Users can configure all 7 projects individually or use the same URL for all.
 */
val remoteModule = module {

    // Dynamic Supabase Client Provider - checks config on each access
    single { DynamicSupabaseClientProvider(get()) }
    single<SupabaseClientProvider> { get<DynamicSupabaseClientProvider>() }

    // Sync queue
    single { SyncQueue() }

    // Retry policy
    single { RetryPolicy() }

    // Remote cache
    single { RemoteCache() }

    // Backend Service (abstraction layer) - singleton with lazy provider fetching
    single<ireader.data.backend.BackendService> {
        val dynamicProvider = get<DynamicSupabaseClientProvider>()
        object : ireader.data.backend.BackendService {
            private fun getDelegate(): ireader.data.backend.BackendService {
                val unwrapped = dynamicProvider.getWrappedProvider()
                return if (unwrapped is ireader.data.remote.NoOpSupabaseClientProvider) {
                    ireader.data.backend.NoOpBackendService()
                } else {
                    val supabaseClient = (unwrapped as MultiSupabaseClientProvider).authClient
                    ireader.data.backend.SupabaseBackendService(supabaseClient)
                }
            }

            override suspend fun query(
                table: String,
                filters: Map<String, Any>,
                columns: String,
                orderBy: String?,
                ascending: Boolean,
                limit: Int?,
                offset: Int?
            ): Result<List<JsonElement>> {
                return getDelegate().query(table, filters, columns, orderBy, ascending, limit, offset)
            }

            override suspend fun insert(table: String, data: JsonElement, returning: Boolean): Result<JsonElement?> {
                return getDelegate().insert(table, data, returning)
            }

            override suspend fun update(table: String, filters: Map<String, Any>, data: JsonElement, returning: Boolean): Result<JsonElement?> {
                return getDelegate().update(table, filters, data, returning)
            }

            override suspend fun delete(table: String, filters: Map<String, Any>): Result<Unit> {
                return getDelegate().delete(table, filters)
            }

            override suspend fun rpc(function: String, parameters: Map<String, Any>): Result<JsonElement> {
                return getDelegate().rpc(function, parameters)
            }

            override suspend fun upsert(table: String, data: JsonElement, onConflict: String?, returning: Boolean): Result<JsonElement?> {
                return getDelegate().upsert(table, data, onConflict, returning)
            }
        }
    }

    // Auth Service (authentication abstraction) - singleton with lazy provider fetching
    single<ireader.data.backend.AuthService> {
        val dynamicProvider = get<DynamicSupabaseClientProvider>()
        object : ireader.data.backend.AuthService {
            private fun getDelegate(): ireader.data.backend.AuthService {
                val unwrapped = dynamicProvider.getWrappedProvider()
                return if (unwrapped is ireader.data.remote.NoOpSupabaseClientProvider) {
                    ireader.data.backend.NoOpAuthService()
                } else {
                    val supabaseClient = (unwrapped as MultiSupabaseClientProvider).authClient
                    ireader.data.backend.SupabaseAuthService(supabaseClient)
                }
            }

            override suspend fun signUp(email: String, password: String): Result<AuthUser> {
                return getDelegate().signUp(email, password)
            }

            override suspend fun signIn(email: String, password: String): Result<AuthUser> {
                return getDelegate().signIn(email, password)
            }

            override suspend fun signOut(): Result<Unit> {
                return getDelegate().signOut()
            }

            override suspend fun getCurrentUser(): Result<AuthUser?> {
                return getDelegate().getCurrentUser()
            }

            override suspend fun getCurrentUserId(): String? {
                return getDelegate().getCurrentUserId()
            }

            override suspend fun isAuthenticated(): Boolean {
                return getDelegate().isAuthenticated()
            }

            override suspend fun sendPasswordReset(email: String): Result<Unit> {
                return getDelegate().sendPasswordReset(email)
            }

            override suspend fun updateEmail(newEmail: String): Result<Unit> {
                return getDelegate().updateEmail(newEmail)
            }

            override suspend fun updatePassword(newPassword: String): Result<Unit> {
                return getDelegate().updatePassword(newPassword)
            }

            override suspend fun refreshToken(): Result<Unit> {
                return getDelegate().refreshToken()
            }
        }
    }

    // Remote repository - singleton with lazy provider fetching
    single<RemoteRepository> {
        val dynamicProvider = get<DynamicSupabaseClientProvider>()
        val syncQueue = get<SyncQueue>()
        val retryPolicy = get<RetryPolicy>()
        val cache = get<RemoteCache>()

        object : RemoteRepository {
            private fun getDelegate(): RemoteRepository {
                val unwrapped = dynamicProvider.getWrappedProvider()
                return if (unwrapped is ireader.data.remote.NoOpSupabaseClientProvider) {
                    ireader.data.remote.NoOpRemoteRepository()
                } else {
                    val supabaseClient = (unwrapped as MultiSupabaseClientProvider).authClient
                    SupabaseRemoteRepository(
                        supabaseClient = supabaseClient,
                        backendService = get(),
                        syncQueue = syncQueue,
                        retryPolicy = retryPolicy,
                        cache = cache
                    )
                }
            }

            override suspend fun signUp(email: String, password: String): Result<ireader.domain.models.remote.User> {
                return getDelegate().signUp(email, password)
            }

            override suspend fun signIn(email: String, password: String): Result<ireader.domain.models.remote.User> {
                return getDelegate().signIn(email, password)
            }

            override suspend fun getCurrentUser(): Result<ireader.domain.models.remote.User?> {
                return getDelegate().getCurrentUser()
            }

            override suspend fun signOut() {
                return getDelegate().signOut()
            }

            override suspend fun updateUsername(userId: String, username: String): Result<Unit> {
                return getDelegate().updateUsername(userId, username)
            }

            override suspend fun updateEthWalletAddress(userId: String, ethWalletAddress: String): Result<Unit> {
                return getDelegate().updateEthWalletAddress(userId, ethWalletAddress)
            }

            override suspend fun updatePassword(newPassword: String): Result<Unit> {
                return getDelegate().updatePassword(newPassword)
            }

            override suspend fun getUserById(userId: String): Result<ireader.domain.models.remote.User?> {
                return getDelegate().getUserById(userId)
            }

            override suspend fun syncReadingProgress(progress: ireader.domain.models.remote.ReadingProgress): Result<Unit> {
                return getDelegate().syncReadingProgress(progress)
            }

            override suspend fun getReadingProgress(userId: String, bookId: String): Result<ireader.domain.models.remote.ReadingProgress?> {
                return getDelegate().getReadingProgress(userId, bookId)
            }

            override fun observeReadingProgress(userId: String, bookId: String): kotlinx.coroutines.flow.Flow<ireader.domain.models.remote.ReadingProgress?> {
                return getDelegate().observeReadingProgress(userId, bookId)
            }

            override fun observeConnectionStatus(): kotlinx.coroutines.flow.Flow<ireader.domain.models.remote.ConnectionStatus> {
                return getDelegate().observeConnectionStatus()
            }

            override suspend fun syncBook(book: ireader.domain.models.remote.SyncedBook): Result<Unit> {
                return getDelegate().syncBook(book)
            }

            override suspend fun getSyncedBooks(userId: String): Result<List<ireader.domain.models.remote.SyncedBook>> {
                return getDelegate().getSyncedBooks(userId)
            }

            override suspend fun deleteSyncedBook(userId: String, bookId: String): Result<Unit> {
                return getDelegate().deleteSyncedBook(userId, bookId)
            }
        }
    }

    // Admin User repository for admin user management - singleton with lazy provider fetching
    single<ireader.domain.data.repository.AdminUserRepository> {
        val dynamicProvider = get<DynamicSupabaseClientProvider>()

        object : ireader.domain.data.repository.AdminUserRepository {
            private fun getDelegate(): ireader.domain.data.repository.AdminUserRepository {
                val unwrapped = dynamicProvider.getWrappedProvider()
                return if (unwrapped is ireader.data.remote.NoOpSupabaseClientProvider) {
                    ireader.data.admin.NoOpAdminUserRepository()
                } else {
                    val supabaseClient = (unwrapped as MultiSupabaseClientProvider).authClient
                    ireader.data.admin.AdminUserRepositoryImpl(
                        supabaseClient = supabaseClient,
                        backendService = get()
                    )
                }
            }

            override suspend fun getAllUsers(limit: Int, offset: Int, searchQuery: String?): Result<List<ireader.domain.models.remote.AdminUser>> {
                return getDelegate().getAllUsers(limit, offset, searchQuery)
            }

            override suspend fun getUserById(userId: String): Result<ireader.domain.models.remote.AdminUser?> {
                return getDelegate().getUserById(userId)
            }

            override suspend fun assignBadgeToUser(userId: String, badgeId: String): Result<Unit> {
                return getDelegate().assignBadgeToUser(userId, badgeId)
            }

            override suspend fun removeBadgeFromUser(userId: String, badgeId: String): Result<Unit> {
                return getDelegate().removeBadgeFromUser(userId, badgeId)
            }

            override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
                return getDelegate().sendPasswordResetEmail(email)
            }

            override suspend fun getAvailableBadgesForAssignment(): Result<List<ireader.domain.models.remote.Badge>> {
                return getDelegate().getAvailableBadgesForAssignment()
            }

            override suspend fun getUserBadges(userId: String): Result<List<ireader.domain.models.remote.Badge>> {
                return getDelegate().getUserBadges(userId)
            }

            override suspend fun isCurrentUserAdmin(): Result<Boolean> {
                return getDelegate().isCurrentUserAdmin()
            }
        }
    }
}
