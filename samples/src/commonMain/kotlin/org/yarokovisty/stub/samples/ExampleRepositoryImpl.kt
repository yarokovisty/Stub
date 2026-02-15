package org.yarokovisty.stub.samples

class ExampleRepositoryImpl(
    private val dataSource: ExampleDataSource,
) : ExampleRepository {

    override fun getString(): String =
        dataSource.getString()

    override fun getInt(): Int =
        dataSource.getInt()

    override fun getData(id: Int, name: String): ExampleData =
        dataSource.getData(id, name)
}
