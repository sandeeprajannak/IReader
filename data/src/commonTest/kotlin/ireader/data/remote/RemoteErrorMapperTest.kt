package ireader.data.remote

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteErrorMapperTest {

    @Test
    fun `withErrorMapping preserves an already user-friendly message instead of masking it as service unavailable`() = runTest {
        val result = RemoteErrorMapper.withErrorMapping<Unit> {
            throw Exception("Email confirmation required. Please check your email or contact admin to disable email confirmation.")
        }

        val message = result.exceptionOrNull()?.message
        assertEquals(
            "Email confirmation required. Please check your email or contact admin to disable email confirmation.",
            message
        )
    }

    @Test
    fun `withErrorMapping still maps a genuine maintenance error to a service unavailable message`() = runTest {
        val result = RemoteErrorMapper.withErrorMapping<Unit> {
            throw Exception("503 Service Unavailable - scheduled maintenance")
        }

        val message = result.exceptionOrNull()?.message
        assertEquals(
            "Service temporarily unavailable due to maintenance. Some features may be limited.",
            message
        )
    }
}
