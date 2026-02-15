package org.yarokovisty.stub.samples

import org.yarokovisty.stub.dsl.any
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
        val expected = ExampleData(0, "name")
        every { repository.getString() } returns "mocked data"
        every { repository.getInt() } returns 1
        every { repository.getData(0, "name") } returns ExampleData(0, "name")

        val stringResult = useCase.getString()
        val intResult = useCase.getInt()
        val dataResult = useCase.getData(0, "name")

        assertEquals("mocked data", stringResult)
        assertEquals(1, intResult)
        assertEquals(expected, dataResult)
    }

    @Test
    fun verifiesMethodWasCalled() {
        every { repository.getString() } returns "mocked data"
        every { repository.getInt() } returns 1
        every { repository.getData(0, "name") } returns ExampleData(0, "name")

        useCase.getString()
        useCase.getInt()
        useCase.getData(0, "name")

        verify { repository.getString() }
        verify { repository.getInt() }
        verify { repository.getData(0, "name") }
    }

    @Test
    fun throwsWhenCalledWithWrongArguments() {
        every { repository.getData(1, "value") } returns ExampleData(1, "value")

        assertFailsWith<IllegalStateException> {
            useCase.getData(0, "name")
        }
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
        every { repository.getString() } returns "data"

        assertFailsWith<IllegalStateException> {
            verify { repository.getString() }
        }
    }

    @Test
    fun anyMatcherMatchesAllArguments() {
        every { repository.getData(any(), any()) } returns ExampleData(0, "any")

        val result1 = useCase.getData(10, "foo")
        val result2 = useCase.getData(20, "bar")

        assertEquals(ExampleData(0, "any"), result1)
        assertEquals(ExampleData(0, "any"), result2)
    }

    @Test
    fun mixedAnyAndEqNotSupported() {
        every { repository.getData(any(), any()) } returns ExampleData(0, "wildcard")
        every { repository.getData(42, "specific") } returns ExampleData(42, "specific")

        assertEquals(ExampleData(42, "specific"), useCase.getData(42, "specific"))
        assertEquals(ExampleData(0, "wildcard"), useCase.getData(99, "other"))
    }
}
