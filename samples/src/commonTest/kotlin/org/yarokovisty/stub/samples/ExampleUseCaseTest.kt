package org.yarokovisty.stub.samples

import org.yarokovisty.stub.dsl.every
import org.yarokovisty.stub.dsl.stub
import org.yarokovisty.stub.dsl.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExampleUseCaseTest {

    private val repository: ExampleRepository = stub()
    private val useCase = ExampleUseCase(repository)

    @Test
    fun returnsConfiguredValue() {
        every { repository.get() } returns "mocked data"

        val result = useCase.invoke()

        assertEquals("mocked data", result)
    }

    @Test
    fun verifiesMethodWasCalled() {
        every { repository.get() } returns "data"

        useCase.invoke()

        verify { repository.get() }
    }

    @Test
    fun throwsConfiguredException() {
        every { repository.get() } throws IllegalArgumentException("test error")

        assertFailsWith<IllegalArgumentException> {
            useCase.invoke()
        }
    }

    @Test
    fun answersWithLambda() {
        every { repository.get() } answers { "dynamic result" }

        val result = useCase.invoke()

        assertEquals("dynamic result", result)
    }

    @Test
    fun failsWhenNoAnswerConfigured() {
        val unconfiguredRepo: ExampleRepository = stub()
        val unconfiguredUseCase = ExampleUseCase(unconfiguredRepo)

        assertFailsWith<IllegalStateException> {
            unconfiguredUseCase.invoke()
        }
    }

    @Test
    fun failsVerificationWhenNotCalled() {
        every { repository.get() } returns "data"

        assertFailsWith<IllegalStateException> {
            verify { repository.get() }
        }
    }
}
