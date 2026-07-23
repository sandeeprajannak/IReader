package ireader.domain.usecases.chapter.controller

import ireader.domain.models.entities.Track
import ireader.domain.models.entities.TrackStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MyNovelListProgressPushTest {

    private fun plannedTrack() = Track(
        id = 1,
        mangaId = 10,
        siteId = 7,
        entryId = 100,
        lastRead = 0f,
        totalChapters = 12,
        status = TrackStatus.Planned,
        startReadTime = 0,
        endReadTime = 0
    )

    @Test
    fun `first chapter read moves status to Reading and stamps start time`() {
        val update = buildMyNovelListProgressUpdate(
            track = plannedTrack(),
            currentChapter = 1,
            totalChapters = 12,
            nowEpochMillis = 5000L
        )

        requireNotNull(update)
        assertEquals(TrackStatus.Reading, update.status)
        assertEquals(1f, update.lastRead)
        assertEquals(5000L, update.startReadTime)
        assertEquals(0L, update.endReadTime)
    }

    @Test
    fun `later chapter read bumps progress without touching an already-set start time`() {
        val reading = plannedTrack().copy(status = TrackStatus.Reading, lastRead = 2f, startReadTime = 1000L)

        val update = buildMyNovelListProgressUpdate(
            track = reading,
            currentChapter = 3,
            totalChapters = 12,
            nowEpochMillis = 9000L
        )

        requireNotNull(update)
        assertEquals(TrackStatus.Reading, update.status)
        assertEquals(3f, update.lastRead)
        assertEquals(1000L, update.startReadTime)
    }

    @Test
    fun `reading the last chapter marks the track Completed and stamps end time`() {
        val reading = plannedTrack().copy(status = TrackStatus.Reading, lastRead = 11f, startReadTime = 1000L)

        val update = buildMyNovelListProgressUpdate(
            track = reading,
            currentChapter = 12,
            totalChapters = 12,
            nowEpochMillis = 9000L
        )

        requireNotNull(update)
        assertEquals(TrackStatus.Completed, update.status)
        assertEquals(12f, update.lastRead)
        assertEquals(9000L, update.endReadTime)
    }

    @Test
    fun `re-reading an already-recorded chapter produces no update`() {
        val reading = plannedTrack().copy(status = TrackStatus.Reading, lastRead = 5f, startReadTime = 1000L)

        val update = buildMyNovelListProgressUpdate(
            track = reading,
            currentChapter = 3,
            totalChapters = 12,
            nowEpochMillis = 9000L
        )

        assertNull(update)
    }
}
