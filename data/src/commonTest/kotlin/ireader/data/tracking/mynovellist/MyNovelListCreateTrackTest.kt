package ireader.data.tracking.mynovellist

import ireader.domain.models.entities.Book
import ireader.domain.models.entities.TrackStatus
import ireader.domain.models.entities.TrackerService
import kotlin.test.Test
import kotlin.test.assertEquals

class MyNovelListCreateTrackTest {

    @Test
    fun `buildTrackFromBook maps book metadata and source url into a plan-to-read track`() {
        val book = Book(
            sourceId = 1,
            title = "Solo Leveling",
            key = "solo-leveling",
            author = "Chugong",
            genres = listOf("Action", "Fantasy"),
            cover = "https://example.com/cover.jpg"
        )

        val track = buildTrackFromBook(
            bookId = 42,
            book = book,
            sourceUrl = "https://novelsite.com/solo-leveling",
            totalChapters = 270
        )

        assertEquals(42, track.mangaId)
        assertEquals(TrackerService.MYNOVELLIST, track.siteId)
        assertEquals("Solo Leveling", track.title)
        assertEquals("https://novelsite.com/solo-leveling", track.mediaUrl)
        assertEquals(270, track.totalChapters)
        assertEquals(TrackStatus.Planned, track.status)
        assertEquals("Chugong", track.author)
        assertEquals(listOf("Action", "Fantasy"), track.genres)
        assertEquals("https://example.com/cover.jpg", track.coverUrl)
    }
}
