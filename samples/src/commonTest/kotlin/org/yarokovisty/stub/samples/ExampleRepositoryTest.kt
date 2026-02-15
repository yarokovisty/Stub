package org.yarokovisty.stub.samples

import org.yarokovisty.stub.dsl.every
import org.yarokovisty.stub.dsl.stub
import org.yarokovisty.stub.dsl.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class ExampleRepositoryTest {

    private val dataSource: ExampleDataSource = stub()
    private val repository = ExampleRepositoryImpl(dataSource)

    @Test
    fun returnsConfiguredValue() {
        every { dataSource.get() } returns "mocked data"

        val result = repository.get()

        assertEquals("mocked data", result)
    }

    @Test
    fun verifiesMethodWasCalled() {
        every { dataSource.get() } returns "mocked data"

        repository.get()

        verify { dataSource.get() }
    }
}
