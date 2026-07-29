package ireader.domain.catalogs.interactor

import ireader.domain.catalogs.service.CatalogRemoteRepository
import ireader.domain.models.entities.CatalogRemote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetRemoteCatalogsTest {

    private fun catalog(pkgName: String) = CatalogRemote(
        sourceId = pkgName.hashCode().toLong(),
        source = pkgName.hashCode().toLong(),
        name = pkgName,
        description = "",
        pkgName = pkgName,
        versionName = "1.0",
        versionCode = 1,
        lang = "en",
        pkgUrl = "",
        iconUrl = "",
        jarUrl = "",
        nsfw = false,
    )

    private class FakeCatalogRemoteRepository(private val catalogs: List<CatalogRemote>) : CatalogRemoteRepository {
        override suspend fun getRemoteCatalogs(): List<CatalogRemote> = catalogs
        override fun getRemoteCatalogsFlow(): Flow<List<CatalogRemote>> = flowOf(catalogs)
        override suspend fun insertRemoteCatalogs(catalogs: List<CatalogRemote>) {}
        override suspend fun deleteAllRemoteCatalogs() {}
    }

    @Test
    fun `subscribe restricts remote catalogs to the allowed pkgName set`() = runTest {
        val repository = FakeCatalogRemoteRepository(
            listOf(
                catalog("ireader.royalroad.en"),
                catalog("ireader.freewebnovel.en"),
                catalog("ireader.someothersource.en"),
            )
        )
        val getRemoteCatalogs = GetRemoteCatalogs(repository)

        val result = getRemoteCatalogs.subscribe(allowedPkgNames = setOf("ireader.royalroad.en", "ireader.freewebnovel.en")).first()

        assertEquals(setOf("ireader.royalroad.en", "ireader.freewebnovel.en"), result.map { it.pkgName }.toSet())
    }

    @Test
    fun `subscribe returns all remote catalogs when no allowlist is given`() = runTest {
        val repository = FakeCatalogRemoteRepository(
            listOf(
                catalog("ireader.royalroad.en"),
                catalog("ireader.freewebnovel.en"),
            )
        )
        val getRemoteCatalogs = GetRemoteCatalogs(repository)

        val result = getRemoteCatalogs.subscribe().first()

        assertEquals(2, result.size)
    }
}
