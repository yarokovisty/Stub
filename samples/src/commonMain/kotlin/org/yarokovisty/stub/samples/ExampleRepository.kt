package org.yarokovisty.stub.samples

interface ExampleRepository {

    fun getString(): String

    fun getInt(): Int

    fun getData(id: Int, name: String): ExampleData
}
