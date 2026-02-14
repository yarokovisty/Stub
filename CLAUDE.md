# Project: Stub

You are working on a production-ready Kotlin Multiplatform (KMP) mocking framework called Stub.

The goal of Stub is to provide a lightweight, compiler-plugin–based mocking solution for unit testing in Kotlin Multiplatform projects.

---

## Core Goals

1. Provide `stub<T>()` API for creating stub objects.
2. Provide `every { ... } returns ...` DSL for mocking method calls.
3. Support interfaces and final classes (final classes must be handled via compiler plugin code generation).
4. Support JVM, Native, and JS platforms.
5. Avoid runtime reflection on Native/JS.
6. Use a compiler-plugin-based approach to generate stub classes for final classes and interfaces.
7. The runtime engine must be deterministic, thread-safe, minimal, and KMP-compatible.
8. Be production-ready and publishable to GitHub/Maven.

---

## DSL Design Rule

DSL must look like:
every { dataSource.load() } returns "mocked"

## Developer Guidance

- Output **actual Kotlin code** for each module as you develop.
- Provide **step-by-step development instructions**, including build.gradle.kts configuration for KMP.
- Make sure DSL, runtime, and compiler plugin integrate cleanly.
- Suggest improvements to maintain KMP-first, production-ready architecture.
- Focus on **fully working MVP** first, then extend with advanced features like returnsMany, verify, coroutine support.

---

Claude, you will act as a **developer subagent** that writes code, builds modules, and outputs actionable development steps. Always provide **working Kotlin code** with explanations for each step. After completing each step, commit and push using git-agent.