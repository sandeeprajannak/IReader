package ireader.data.tracking.mynovellist

import ireader.domain.models.entities.Track
import ireader.domain.models.entities.TrackStatus
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MyNovelListApiPayloadTest {

    private fun baseTrack() = Track(
        mangaId = 1,
        siteId = 7,
        entryId = 0,
        title = "Solo Leveling",
        mediaUrl = "https://mynoveltracker.netlify.app/novel/solo-leveling",
        totalChapters = 270,
        lastRead = 42f,
        score = 8.5f,
        status = TrackStatus.Reading
    )

    @Test
    fun `create payload omits optional metadata when blank`() {
        val payload = buildCreatePayload(baseTrack())

        assertEquals("Solo Leveling", payload["title"]?.jsonPrimitive?.content)
        assertNull(payload["author"])
        assertNull(payload["cover_url"])
        assertNull(payload["tags"])
    }

    @Test
    fun `create payload includes author cover and tags when present`() {
        val track = baseTrack().copy(
            author = "Chugong",
            coverUrl = "https://example.com/cover.jpg",
            genres = listOf("Action", "Fantasy")
        )

        val payload = buildCreatePayload(track)

        assertEquals("Chugong", payload["author"]?.jsonPrimitive?.content)
        assertEquals("https://example.com/cover.jpg", payload["cover_url"]?.jsonPrimitive?.content)
        assertEquals(
            listOf("Action", "Fantasy"),
            payload["tags"]?.jsonArray?.map { it.jsonPrimitive.content }
        )
    }

    @Test
    fun `progress payload always resends total chapters`() {
        val payload = buildProgressPayload(baseTrack())

        assertEquals(270, payload["total_chapters"]?.jsonPrimitive?.int)
    }

    @Test
    fun `progress payload omits notes when blank but includes when present`() {
        val withoutNotes = buildProgressPayload(baseTrack())
        assertNull(withoutNotes["notes"])

        val withNotes = buildProgressPayload(baseTrack().copy(notes = "Great pacing so far"))
        assertEquals("Great pacing so far", withNotes["notes"]?.jsonPrimitive?.content)
    }
}
