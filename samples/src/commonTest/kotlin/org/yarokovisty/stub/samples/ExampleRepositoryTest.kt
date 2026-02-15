package org.yarokovisty.stub.samples

import kotlinx.coroutines.test.runTest
import org.yarokovisty.stub.dsl.any
import org.yarokovisty.stub.dsl.coEvery
import org.yarokovisty.stub.dsl.eq
import org.yarokovisty.stub.dsl.every
import org.yarokovisty.stub.dsl.stub
import org.yarokovisty.stub.dsl.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExampleRepositoryTest {

    private val dataSource: ExampleDataSource = stub()
    private val repository = ExampleRepositoryImpl(dataSource)

    @Test
    fun returnsConfiguredValue() {
        val expected = ExampleData(0, "name")
        every { dataSource.getString() } answers "mocked data"
        every { dataSource.getInt() } answers 0
        every { dataSource.getData(0, "name") } answers ExampleData(0, "name")

        val stringResult = repository.getString()
        val intResult = repository.getInt()
        val dataResult = repository.getData(0, "name")

        assertEquals("mocked data", stringResult)
        assertEquals(0, intResult)
        assertEquals(expected, dataResult)
    }

    @Test
    fun verifiesMethodWasCalled() {
        every { dataSource.getString() } answers "mocked data"
        every { dataSource.getInt() } answers 0
        every { dataSource.getData(0, "name") } answers ExampleData(0, "name")

        repository.getString()
        repository.getInt()
        repository.getData(0, "name")

        verify { dataSource.getString() }
        verify { dataSource.getInt() }
        verify { dataSource.getData(0, "name") }
    }

    @Test
    fun throwsWhenCalledWithWrongArguments() {
        every { dataSource.getData(1, "value") } answers ExampleData(1, "value")

        assertFailsWith<IllegalStateException> {
            repository.getData(0, "name")
        }
    }

    @Test
    fun returnsConfiguredValueForSuspendFunction() = runTest {
        coEvery { dataSource.getData() } answers ExampleData(0, "value")

        val result = repository.getData()

        assertEquals(ExampleData(0, "value"), result)
    }

    @Test
    fun anyMatcherMatchesAllArguments() {
        every { dataSource.getData(any(), any()) } answers ExampleData(99, "any")

        val result1 = repository.getData(1, "a")
        val result2 = repository.getData(2, "b")

        assertEquals(ExampleData(99, "any"), result1)
        assertEquals(ExampleData(99, "any"), result2)
    }

    @Test
    fun eqMatcherMatchesExactArguments() {
        every { dataSource.getData(eq(5), eq("test")) } answers ExampleData(5, "test")

        val result = repository.getData(5, "test")

        assertEquals(ExampleData(5, "test"), result)
        assertFailsWith<IllegalStateException> {
            repository.getData(5, "other")
        }
    }

    @Test
    fun lastRegisteredAnswerWins() {
        every { dataSource.getData(any(), any()) } answers ExampleData(0, "fallback")
        every { dataSource.getData(1, "special") } answers ExampleData(1, "special")

        val specific = repository.getData(1, "special")
        val fallback = repository.getData(2, "other")

        assertEquals(ExampleData(1, "special"), specific)
        assertEquals(ExampleData(0, "fallback"), fallback)
    }
}
