package org.yarokovisty.stub.samples

import org.yarokovisty.stub.dsl.every
import org.yarokovisty.stub.dsl.stub
import org.yarokovisty.stub.dsl.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ExampleUseCaseTest {

    private val repository: ExampleRepository = stub()
    private val useCase = ExampleUseCase(repository)

    @Test
    fun returnsConfiguredValue() {
        val expected = ExampleData(0, "name")
        every { repository.getString() } answers "mocked data"
        every { repository.getInt() } answers 1
        every { repository.getData(0, "name") } answers ExampleData(0, "name")

        val stringResult = useCase.getString()
        val intResult = useCase.getInt()
        val dataResult = useCase.getData(0, "name")

        assertEquals("mocked data", stringResult)
        assertEquals(1, intResult)
        assertEquals(expected, dataResult)
    }

    @Test
    fun verifiesMethodWasCalled() {
        every { repository.getString() } answers "mocked data"
        every { repository.getInt() } answers 1
        every { repository.getData(0, "name") } answers ExampleData(0, "name")

        useCase.getString()
        useCase.getInt()
        useCase.getData(0, "name")

        verify { repository.getString() }
        verify { repository.getInt() }
        verify { repository.getData(0, "name") }
    }

    @Test
    fun returnsWrongConfiguredWhenWrongInputParameters() {
        val expected = ExampleData(0, "name")
        every { repository.getData(1, "value") } answers ExampleData(1, "value")

        val dataResult = useCase.getData(0, "name")

        assertNotEquals(expected, dataResult)
    }

    @Test
    fun throwsConfiguredException() {
        every { repository.getString() } throws IllegalArgumentException("test error")

        assertFailsWith<IllegalArgumentException> {
            useCase.getString()
        }
    }

    @Test
    fun answersWithLambda() {
        every { repository.getString() } answers { "dynamic result" }

        val result = useCase.getString()

        assertEquals("dynamic result", result)
    }

    @Test
    fun failsWhenNoAnswerConfigured() {
        val unconfiguredRepo: ExampleRepository = stub()
        val unconfiguredUseCase = ExampleUseCase(unconfiguredRepo)

        assertFailsWith<IllegalStateException> {
            unconfiguredUseCase.getString()
        }
    }

    @Test
    fun failsVerificationWhenNotCalled() {
        every { repository.getString() } answers "data"

        assertFailsWith<IllegalStateException> {
            verify { repository.getString() }
        }
    }
}
