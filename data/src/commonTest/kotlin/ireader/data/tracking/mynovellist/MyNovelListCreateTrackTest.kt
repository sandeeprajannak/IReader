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

    @Test
    fun `boundToMyNovelListEntry rewrites mediaUrl to the MyNovelList novel page, not the source url`() {
        val track = buildTrackFromBook(
            bookId = 42,
            book = Book(sourceId = 1, title = "Solo Leveling", key = "solo-leveling"),
            sourceUrl = "https://novelsite.com/solo-leveling",
            totalChapters = 270
        )
        val entry = MyNovelListEntry(
            id = "1d9ec407-0aa6-49a4-bf89-5d7e1ba8b87d",
            title = "Solo Leveling",
            author = null,
            coverUrl = null,
            sourceUrl = "https://novelsite.com/solo-leveling",
            totalChapters = 270,
            status = "planning",
            currentChapter = 0,
            score = 0,
            startedAt = null,
            completedAt = null
        )

        val bound = track.boundToMyNovelListEntry(entry, "https://mynovellist.example.com")

        assertEquals(
            "https://mynovellist.example.com/novel/1d9ec407-0aa6-49a4-bf89-5d7e1ba8b87d",
            bound.mediaUrl
        )
        assertEquals(entry.id.hashCode().toLong(), bound.entryId)
    }
}
