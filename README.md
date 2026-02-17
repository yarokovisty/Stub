# Stub

A lightweight, compiler-plugin-based mocking framework for **Kotlin Multiplatform**. Stub generates mock implementations at compile time, enabling type-safe stubbing and verification across JVM, Android, iOS, and JS — with zero runtime reflection.

## Key Features

- **`stub<T>()`** — create stubs for interfaces and final classes with a single call
- **Expressive DSL** — `every { mock.call() } returns value` syntax inspired by MockK
- **Argument matchers** — `any()`, `eq()` for flexible argument matching
- **Coroutine support** — `coEvery` / `coVerify` for suspend functions
- **Final class mocking** — no `open` keyword required; handled at the FIR compiler phase
- **Truly multiplatform** — no reflection on Native/JS; all stubs are generated at compile time
- **Kotlin 2.2.20** — built on the K2 compiler with FIR and IR extensions

## Supported Platforms

| Platform | Target |
|---|---|
| JVM | `jvm` |
| Android | `androidTarget` (minSdk 24) |
| iOS | `iosArm64`, `iosSimulatorArm64`, `iosX64` |
| JavaScript | `js` (Node.js) |

---

## Getting Started

### Installation

Stub is distributed as a set of Gradle modules. Add the compiler plugin and DSL dependency to your project:

**settings.gradle.kts**

```kotlin
includeBuild("build-logic")
include(":stub:compiler-plugin")
include(":stub:dsl")
include(":stub:runtime")
```

**your-module/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.stubCompiler)            // applies the compiler plugin
    alias(libs.plugins.stubKotlinMultiplatform)
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(project(":stub:dsl"))          // includes :stub:runtime transitively
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)  // only if using coEvery/coVerify
        }
    }
}
```

> The `stub:dsl` module declares `api(project(":stub:runtime"))`, so you only need to depend on `:stub:dsl`.

### Quick Example

```kotlin
import org.yarokovisty.stub.dsl.*
import kotlin.test.Test
import kotlin.test.assertEquals

interface UserRepository {
    fun findById(id: Int): String
}

class UserRepositoryTest {

    private val repository: UserRepository = stub()

    @Test
    fun returnsConfiguredValue() {
        every { repository.findById(1) } returns "Alice"

        assertEquals("Alice", repository.findById(1))

        verify { repository.findById(1) }
    }
}
```

That's it — no annotations, no manual mock classes, no `open` modifiers.

---

## Core Concepts

### Creating Stubs

Use `stub<T>()` to create a stub instance of any interface or class:

```kotlin
// Stub an interface
val repository: UserRepository = stub()

// Stub a final class — no `open` keyword needed
val dataSource: DataSource = stub()
```

The compiler plugin intercepts `stub<T>()` calls and generates a synthetic class that:

1. Implements the target interface (or extends the target class)
2. Delegates every method call to an internal `StubDelegate`
3. Supports configurable return values, exceptions, and verification

### Configuring Behavior

#### `returns` — return a fixed value

```kotlin
every { repository.findById(1) } returns "Alice"
```

#### `throws` — throw an exception

```kotlin
every { repository.findById(-1) } throws IllegalArgumentException("Invalid ID")
```

#### `answers` — compute a return value dynamically

```kotlin
every { repository.findById(any()) } answers { call ->
    val id = call.args[0] as Int
    "User #$id"
}
```

The `answers` lambda receives a `MethodCall` object with `methodName` and `args`.

### Verification

Assert that a method was called with the expected arguments:

```kotlin
every { repository.findById(1) } returns "Alice"
repository.findById(1)

verify { repository.findById(1) }
```

If the method was **not** called, `verify` throws an `IllegalStateException`.

### Clearing State

```kotlin
// Clear all configured answers AND recorded call history
clearStubs(repository)

// Clear only the recorded call history (keep answers intact)
clearInvocations(repository)
```

---

## DSL Usage

### Argument Matchers

Stub provides two argument matchers for flexible stubbing and verification:

| Matcher | Description |
|---|---|
| `any<T>()` | Matches any argument value |
| `eq(value)` | Matches only values equal to `value` |

#### Wildcard matching with `any()`

```kotlin
every { dataSource.getData(any(), any()) } returns ExampleData(0, "fallback")

dataSource.getData(1, "a")  // returns ExampleData(0, "fallback")
dataSource.getData(2, "b")  // returns ExampleData(0, "fallback")
```

#### Exact matching with `eq()`

```kotlin
every { dataSource.getData(eq(5), eq("test")) } returns ExampleData(5, "test")

dataSource.getData(5, "test")   // returns ExampleData(5, "test")
dataSource.getData(5, "other")  // throws MissingAnswerException
```

#### Implicit exact matching

When no matchers are used, all arguments are matched by exact equality:

```kotlin
// These are equivalent:
every { dataSource.getData(1, "name") } returns result
every { dataSource.getData(eq(1), eq("name")) } returns result
```

#### Answer priority

The **last registered answer wins**. This allows you to set up a fallback and then override specific cases:

```kotlin
every { dataSource.getData(any(), any()) } returns ExampleData(0, "fallback")
every { dataSource.getData(1, "special") } returns ExampleData(1, "special")

dataSource.getData(1, "special")  // ExampleData(1, "special") — specific match
dataSource.getData(2, "other")    // ExampleData(0, "fallback") — wildcard fallback
```

### Stubbing Interfaces

Interfaces are stubbed directly — no special configuration needed:

```kotlin
interface ExampleRepository {
    fun getString(): String
    fun getData(id: Int, name: String): ExampleData
    suspend fun getData(): ExampleData
}

val repository: ExampleRepository = stub()
every { repository.getString() } returns "mocked"
```

### Stubbing Final Classes

Final classes are stubbed the same way. The compiler plugin opens classes, functions, and properties at the FIR phase, so no `open` keyword is required in your production code:

```kotlin
class ExampleDataSource(private val httpClient: HttpClient) {
    fun getString(): String = httpClient.getString()
    fun getData(id: Int, name: String): ExampleData = httpClient.getData(id, name)
}

val dataSource: ExampleDataSource = stub()
every { dataSource.getString() } returns "mocked"
```

The plugin automatically provides default values for non-nullable constructor parameters when generating the stub class.

---

## Advanced Usage

### Coroutine Support

Use `coEvery` and `coVerify` for suspend functions:

```kotlin
import kotlinx.coroutines.test.runTest

@Test
fun testSuspendFunction() = runTest {
    val repository: ExampleRepository = stub()

    coEvery { repository.getData() } returns ExampleData(1, "async")

    val result = repository.getData()

    assertEquals(ExampleData(1, "async"), result)
    coVerify { repository.getData() }
}
```

`coEvery` and `coVerify` must be called from a suspend context (e.g., inside `runTest`).

### Unconfigured Method Calls

If a stubbed method is called without a configured answer, a `MissingAnswerException` is thrown:

```kotlin
val repository: ExampleRepository = stub()

// Throws: No answer configured for method 'getString'.
// Use every { } returns ... to configure.
repository.getString()
```

This fail-fast behavior ensures your tests are explicit about expected interactions.

### Verification Failure

If `verify` is called for a method that was not invoked, it throws `IllegalStateException`:

```kotlin
every { repository.getString() } returns "data"

// Throws: Method 'getString' was never called
verify { repository.getString() }
```

---

## Runtime & Compiler Plugin

### Architecture

Stub consists of three modules:

```
stub:runtime    Core engine (StubDelegate, MockRecorder, Matcher, Answer)
stub:dsl        User-facing API (every, verify, coEvery, coVerify, stub<T>())
stub:compiler-plugin   Kotlin compiler plugin (FIR + IR extensions)
```

### Runtime Engine

- **`StubDelegate`** — thread-safe call handler that stores configured answers and records method invocations. Uses `AtomicReference` for lock-free operation across all platforms.
- **`MockRecorder`** — singleton that captures method calls during `every { }` and `verify { }` recording blocks.
- **`MatcherStack`** — thread-safe stack that collects argument matchers pushed by `any()` and `eq()` calls.
- **`Answer`** — sealed interface with three variants: `Value`, `Throwing`, and `Lambda`.

### Compiler Plugin

The compiler plugin operates in two phases:

#### FIR Phase — `StubFirStatusTransformer`

Opens all non-sealed classes, functions, and properties by changing their modality from `FINAL` to `OPEN`. This happens at the FIR (Frontend IR) level, before IR lowering, which is the only reliable way to open final classes in the K2 compiler.

#### IR Phase — `CreateStubTransformer`

1. Intercepts calls to `stub<T>()` (specifically the `createStub` function in `org.yarokovisty.stub.dsl`)
2. Reads the type argument `T` from the call site
3. Generates a synthetic `Stub__T` class that:
   - Extends the target class or implements the interface
   - Implements `Stubbable` with a `StubDelegate` field
   - Overrides all public functions and properties, delegating to `stubDelegate.handle()`
4. Replaces the `createStub(...)` call with a direct constructor call to `Stub__T()`
5. Caches generated classes per FQN to avoid duplicate generation

### Why Compile-Time Generation?

| Approach | JVM | Native | JS |
|---|---|---|---|
| Runtime reflection | Works | Not available | Not available |
| Dynamic proxy | Works | Not available | Not available |
| **Compile-time generation** | **Works** | **Works** | **Works** |

By generating stubs at compile time, Stub works identically across all Kotlin/Multiplatform targets without platform-specific code.

---

## Best Practices

### Keep stubs focused

Configure only the methods your test actually exercises. The fail-fast `MissingAnswerException` ensures unused methods are not silently ignored:

```kotlin
// Good — explicit about what this test needs
every { repository.findById(1) } returns "Alice"

// Avoid — configuring methods unrelated to the test
every { repository.findById(any()) } returns "Alice"
every { repository.findAll() } returns emptyList()
every { repository.count() } returns 1
```

### Use `any()` for irrelevant arguments

When the test doesn't care about specific argument values, use `any()` for clarity:

```kotlin
every { logger.log(any(), any()) } returns Unit
```

### Use the last-registered-wins pattern for fallbacks

Set up a wildcard fallback first, then override specific cases:

```kotlin
every { repository.findById(any()) } returns "Unknown"
every { repository.findById(eq(1)) } returns "Alice"
every { repository.findById(eq(2)) } returns "Bob"
```

### Clear state between tests

If you reuse stub instances across tests, call `clearStubs()` or `clearInvocations()` in a setup method to avoid state leakage:

```kotlin
@BeforeTest
fun setUp() {
    clearStubs(repository)
}
```

### Prefer interfaces for test boundaries

While Stub supports final class mocking, designing your code with interfaces at module boundaries leads to cleaner tests and better separation of concerns.

---

## Troubleshooting

### "Stub compiler plugin is not applied"

**Error:** `Stub compiler plugin is not applied.`

**Cause:** The `stub<T>()` call reached the default `createStub()` implementation, which means the compiler plugin did not transform it.

**Fix:** Ensure the compiler plugin is applied in your `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.stubCompiler)
}
```

### `MissingAnswerException`

**Error:** `No answer configured for method 'methodName'. Use every { } returns ... to configure.`

**Cause:** A stubbed method was called without configuring a return value.

**Fix:** Add an `every { ... } returns ...` block for the method before calling it.

### Verification fails — method was never called

**Error:** `IllegalStateException: Method 'methodName' was never called`

**Cause:** The `verify { }` block asserts a method was called, but it wasn't.

**Fix:** Ensure the code under test actually invokes the stubbed method, and that argument values match the verification expectation.

### Argument mismatch with matchers

If you use `any()` for one argument, make sure all arguments in the same call also use matchers:

```kotlin
// Correct — all arguments use matchers
every { service.process(any(), eq("value")) } returns result

// Correct — no matchers (all matched by exact equality)
every { service.process(1, "value") } returns result
```

### Platform-Specific Notes

| Platform | Notes |
|---|---|
| **JVM / Android** | Full support. No additional configuration required. |
| **iOS (Native)** | Full support. No reflection used — all stubs are generated at compile time. |
| **JS (Node.js)** | Full support. Runs on Node.js runtime via Kotlin/JS. |

---

## Requirements

| Dependency | Version |
|---|---|
| Kotlin | 2.2.20 |
| Gradle | 8.14+ |
| kotlinx-coroutines | 1.10.2 (for `coEvery`/`coVerify`) |
| Android Gradle Plugin | 8.12.0 (for Android targets) |
| Android compileSdk | 36 |
| Android minSdk | 24 |

## Running Tests

```bash
# JVM tests only
./gradlew :samples:jvmTest

# All platform tests
./gradlew :samples:allTests

# Static analysis
./gradlew detekt
```

## License

Copyright (c) YarokovistY. All rights reserved.
