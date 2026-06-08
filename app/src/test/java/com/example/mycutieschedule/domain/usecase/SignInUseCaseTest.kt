package com.example.mycutieschedule.domain.usecase

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.fake.FakeAuthRepository
import com.example.mycutieschedule.domain.usecase.auth.SignInUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SignInUseCaseTest {

    private lateinit var repository: FakeAuthRepository
    private lateinit var useCase: SignInUseCase

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        useCase = SignInUseCase(repository)
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `valid email and password returns Success with User`() = runTest {
        val result = useCase("user@example.com", "secret123")

        assertTrue(result is Result.Success)
        assertEquals("user@example.com", (result as Result.Success).data.email)
    }

    // ── Email validation ──────────────────────────────────────────────────────

    @Test
    fun `blank email returns Error`() = runTest {
        val result = useCase("", "secret123")

        assertIsError(result, "email")
    }

    @Test
    fun `email without at-sign returns Error`() = runTest {
        val result = useCase("notanemail", "secret123")

        assertIsError(result, "email")
    }

    @Test
    fun `email without domain returns Error`() = runTest {
        val result = useCase("user@", "secret123")

        assertIsError(result, "email")
    }

    @Test
    fun `email with leading and trailing spaces is trimmed and accepted`() = runTest {
        val result = useCase("  user@example.com  ", "secret123")

        assertTrue(result is Result.Success)
    }

    // ── Password validation ───────────────────────────────────────────────────

    @Test
    fun `blank password returns Error`() = runTest {
        val result = useCase("user@example.com", "")

        assertIsError(result, "password")
    }

    // ── Repository failure ────────────────────────────────────────────────────

    @Test
    fun `repository failure is propagated`() = runTest {
        repository.shouldFailWith = RuntimeException("Wrong password")

        val result = useCase("user@example.com", "wrongpass")

        assertTrue(result is Result.Error)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun assertIsError(result: Result<*>, messageFragment: String) {
        assertTrue("Expected Result.Error but got $result", result is Result.Error)
        val error = result as Result.Error
        assertNotNull(error.message)
        assertTrue(
            "Expected error message to contain '$messageFragment' but was '${error.message}'",
            error.message!!.contains(messageFragment, ignoreCase = true),
        )
    }
}
