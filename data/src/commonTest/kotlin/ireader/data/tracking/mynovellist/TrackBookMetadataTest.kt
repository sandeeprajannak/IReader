package ireader.data.tracking.mynovellist

import ireader.domain.models.entities.Book
import ireader.domain.models.entities.Track
import ireader.domain.models.entities.TrackStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackBookMetadataTest {

    private fun baseTrack() = Track(
        mangaId = 1,
        siteId = 7,
        entryId = 0,
        title = "Solo Leveling",
        status = TrackStatus.Planned
    )

    @Test
    fun `applyBookMetadata leaves track unchanged when book is null`() {
        val track = baseTrack()

        val result = track.applyBookMetadata(null)

        assertEquals(track, result)
    }

    @Test
    fun `applyBookMetadata copies author genres and cover from book`() {
        val book = Book(
            sourceId = 1,
            title = "Solo Leveling",
            key = "solo-leveling",
            author = "Chugong",
            genres = listOf("Action", "Fantasy"),
            cover = "https://example.com/cover.jpg"
        )

        val result = baseTrack().applyBookMetadata(book)

        assertEquals("Chugong", result.author)
        assertEquals(listOf("Action", "Fantasy"), result.genres)
        assertEquals("https://example.com/cover.jpg", result.coverUrl)
    }

    @Test
    fun `applyBookMetadata prefers customCover over cover when set`() {
        val book = Book(
            sourceId = 1,
            title = "Solo Leveling",
            key = "solo-leveling",
            cover = "https://example.com/cover.jpg",
            customCover = "https://example.com/custom-cover.jpg"
        )

        val result = baseTrack().applyBookMetadata(book)

        assertEquals("https://example.com/custom-cover.jpg", result.coverUrl)
    }
}
