package org.yarokovisty.stub.samples

import kotlinx.coroutines.test.runTest
import org.yarokovisty.stub.dsl.coEvery
import org.yarokovisty.stub.dsl.every
import org.yarokovisty.stub.dsl.stub
import org.yarokovisty.stub.dsl.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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
    fun returnsWrongConfiguredWhenWrongInputParameters() {
        val expected = ExampleData(0, "name")
        every { dataSource.getData(1, "value") } answers ExampleData(1, "value")

        val dataResult = repository.getData(0, "name")

        assertNotEquals(expected, dataResult)
    }

    @Test
    fun returnsConfiguredValueForSuspendFunction() = runTest {
        coEvery { dataSource.getData() } answers ExampleData(0, "value")

        val result = repository.getData()

        assertEquals(ExampleData(0, "value"), result)
    }
}
