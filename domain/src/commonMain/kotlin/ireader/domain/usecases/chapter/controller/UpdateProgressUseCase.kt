package ireader.domain.usecases.chapter.controller

import ireader.core.log.Log
import ireader.domain.data.repository.ChapterRepository
import ireader.domain.data.repository.HistoryRepository
import ireader.domain.data.repository.TrackingRepository
import ireader.domain.models.entities.History
import ireader.domain.models.entities.Track
import ireader.domain.models.entities.TrackStatus
import ireader.domain.models.entities.TrackUpdate
import ireader.domain.models.entities.TrackerService
import ireader.domain.preferences.prefs.UiPreferences
import ireader.domain.utils.extensions.currentTimeToLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case interface for updating reading progress.
 * Handles last read chapter tracking and paragraph position persistence.
 */
interface UpdateProgressUseCase {
    /**
     * Update the last read chapter for a book.
     * Records the chapter as the most recently read and marks it as read.
     *
     * @param bookId The unique identifier of the book
     * @param chapterId The unique identifier of the chapter
     */
    suspend fun updateLastRead(bookId: Long, chapterId: Long)

    /**
     * Update the paragraph index (reading position) for a chapter.
     *
     * @param chapterId The unique identifier of the chapter
     * @param paragraphIndex The current paragraph index
     */
    suspend fun updateParagraphIndex(chapterId: Long, paragraphIndex: Int)

    /**
     * Subscribe to the last read chapter ID for a book.
     *
     * @param bookId The unique identifier of the book
     * @return Flow emitting the last read chapter ID when it changes
     */
    fun subscribeLastRead(bookId: Long): Flow<Long?>
}

/**
 * Default implementation of [UpdateProgressUseCase].
 * Delegates to [HistoryRepository] and [ChapterRepository] for persistence.
 */
class UpdateProgressUseCaseImpl(
    private val historyRepository: HistoryRepository,
    private val chapterRepository: ChapterRepository,
    private val uiPreferences: UiPreferences,
    private val trackingRepository: TrackingRepository? = null
) : UpdateProgressUseCase {

    companion object {
        private const val TAG = "UpdateProgressUseCase"
    }

    override suspend fun updateLastRead(bookId: Long, chapterId: Long) {
        // Respect incognito mode
        if (uiPreferences.incognitoMode().get()) {
            return
        }

        val chapter = chapterRepository.findChapterById(chapterId) ?: return
        val existingHistory = historyRepository.findHistoryByChapterId(chapterId)

        Log.debug {
            "$TAG: updateLastRead - chapterId=$chapterId, hasContent=${chapter.content.isNotEmpty()}, " +
            "contentSize=${chapter.content.size}"
        }

        // Mark chapter as read - this preserves content since we fetched with findChapterById
        chapterRepository.insertChapter(
            chapter.copy(read = true)
        )

        // Update history
        historyRepository.insertHistory(
            History(
                id = existingHistory?.id ?: 0,
                chapterId = chapterId,
                readAt = currentTimeToLong(),
                readDuration = existingHistory?.readDuration ?: 0
            )
        )

        pushProgressToMyNovelList(bookId, chapter.number, chapter.sourceOrder, chapter.isRecognizedNumber)
    }

    private suspend fun pushProgressToMyNovelList(
        bookId: Long,
        chapterNumber: Float,
        sourceOrder: Long,
        isRecognizedNumber: Boolean
    ) {
        val repository = trackingRepository ?: return
        try {
            val track = repository.getTracksByBook(bookId).find { it.siteId == TrackerService.MYNOVELLIST } ?: return
            val currentChapter = if (isRecognizedNumber) chapterNumber.toInt() else (sourceOrder + 1).toInt()
            val totalChapters = chapterRepository.findChaptersByBookId(bookId).size
            val update = buildMyNovelListProgressUpdate(track, currentChapter, totalChapters, currentTimeToLong())
            if (update != null) {
                repository.updateTrack(update)
            }
        } catch (e: Exception) {
            Log.error(e, "$TAG: Failed to push reading progress to MyNovelList")
        }
    }

    override suspend fun updateParagraphIndex(chapterId: Long, paragraphIndex: Int) {
        // Respect incognito mode
        if (uiPreferences.incognitoMode().get()) {
            return
        }

        val chapter = chapterRepository.findChapterById(chapterId) ?: return
        
        Log.debug { 
            "$TAG: updateParagraphIndex - chapterId=$chapterId, paragraphIndex=$paragraphIndex, " +
            "hasContent=${chapter.content.isNotEmpty()}"
        }
        
        // Update the lastPageRead field which stores paragraph index
        // This preserves content since we fetched with findChapterById
        chapterRepository.insertChapter(
            chapter.copy(lastPageRead = paragraphIndex.toLong())
        )
    }

    override fun subscribeLastRead(bookId: Long): Flow<Long?> {
        return historyRepository.subscribeHistoryByBookId(bookId)
            .map { history -> history?.chapterId }
    }
}

internal fun buildMyNovelListProgressUpdate(
    track: Track,
    currentChapter: Int,
    totalChapters: Int,
    nowEpochMillis: Long
): TrackUpdate? {
    val isCompleted = totalChapters > 0 && currentChapter >= totalChapters
    val newStatus = when {
        isCompleted -> TrackStatus.Completed
        track.status == TrackStatus.Planned -> TrackStatus.Reading
        else -> track.status
    }
    val newStartReadTime = if (track.startReadTime == 0L) nowEpochMillis else track.startReadTime
    val newEndReadTime = if (isCompleted && track.endReadTime == 0L) nowEpochMillis else track.endReadTime
    val newLastRead = maxOf(currentChapter.toFloat(), track.lastRead)

    val unchanged = newStatus == track.status &&
        newLastRead == track.lastRead &&
        newStartReadTime == track.startReadTime &&
        newEndReadTime == track.endReadTime
    if (unchanged) return null

    return TrackUpdate(
        id = track.id,
        lastRead = newLastRead,
        status = newStatus,
        startReadTime = newStartReadTime,
        endReadTime = newEndReadTime
    )
}
