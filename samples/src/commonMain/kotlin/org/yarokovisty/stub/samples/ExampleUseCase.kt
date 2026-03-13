package org.yarokovisty.stub.samples

class ExampleUseCase(
    private val repository: ExampleRepository,
) {

    fun getString(): String =
        repository.getString()

    fun getInt(): Int =
        repository.getInt()

    fun getData(id: Int, name: String): ExampleData =
        repository.getData(id, name)

    suspend fun getData(): ExampleData =
        repository.getData()
}
