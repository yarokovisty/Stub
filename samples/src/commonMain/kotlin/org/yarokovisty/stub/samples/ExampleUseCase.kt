package org.yarokovisty.stub.samples

class ExampleUseCase(
    private val repository: ExampleRepository,
) {

    fun invoke(): String =
        repository.get()
}
