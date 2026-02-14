package org.yarokovisty.stub.samples

class ExampleRepositoryImpl(
    private val dataSource: ExampleDataSource,
) : ExampleRepository {

    override fun get(): String =
        dataSource.get()
}
