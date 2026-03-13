package org.yarokovisty.stub.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irLong
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isDouble
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isNullable

@OptIn(UnsafeDuringIrConstructionAPI::class)
class DefaultValueFactory(
    private val pluginContext: IrPluginContext,
    private val generateStubForClass: ((IrClass) -> IrClass)? = null,
) {

    private companion object {
        private const val MAX_RECURSION_DEPTH = 3
    }

    @Suppress("MagicNumber")
    fun defaultIrValue(type: IrType, builder: IrBuilderWithScope, depth: Int = 0): IrExpression =
        when {
            type.isNullable() -> builder.irNull()
            depth >= MAX_RECURSION_DEPTH -> createDummyInstance(type, builder)
            type.isInt() -> builder.irInt(0)
            type.isLong() -> builder.irLong(0L)
            type.isFloat() -> IrConstImpl.float(
                builder.startOffset,
                builder.endOffset,
                type,
                0f,
            )
            type.isDouble() -> IrConstImpl.double(
                builder.startOffset,
                builder.endOffset,
                type,
                0.0,
            )
            type.isBoolean() -> builder.irBoolean(false)
            type.isString() -> builder.irString("")
            else -> constructDefaultInstance(type, builder, depth)
        }

    private fun createDummyInstance(type: IrType, builder: IrBuilderWithScope): IrExpression {
        // Create Any() and cast to target type - this satisfies compile-time null-safety
        // while allowing us to create a placeholder for complex types
        val anyConstructor = pluginContext.irBuiltIns.anyClass.owner.constructors.first()
        val anyInstance = builder.irCallConstructor(anyConstructor.symbol, emptyList())
        return builder.irAs(anyInstance, type)
    }

    /**
     * Check if we can safely construct a default value for the given type without risking
     * runtime errors like ClassCastException or NullPointerException.
     */
    fun canConstructSafely(type: IrType, depth: Int = 0): Boolean {
        if (depth >= MAX_RECURSION_DEPTH) return false
        if (type.isNullable()) return true
        if (isPrimitiveType(type)) return true

        val classSymbol = type.classOrNull ?: return false
        val irClass = classSymbol.owner

        // Can't safely construct interfaces, abstract classes, or final classes
        // Final classes are typically from external libraries and can't be extended
        if (irClass.kind == ClassKind.INTERFACE ||
            irClass.kind == ClassKind.ANNOTATION_CLASS ||
            irClass.modality == Modality.FINAL
        ) {
            return false
        }

        // Check if the class has a constructor we can call
        val constructor = irClass.constructors.firstOrNull { it.isPrimary }
            ?: irClass.constructors.firstOrNull()
            ?: return false

        // Check if all constructor parameters can be safely constructed
        return constructor.parameters
            .filter { it.kind == IrParameterKind.Regular }
            .all { param -> canConstructSafely(param.type, depth + 1) }
    }

    private fun isPrimitiveType(type: IrType): Boolean =
        type.isInt() || type.isLong() || type.isFloat() ||
            type.isDouble() || type.isBoolean() || type.isString()


    @Suppress("ReturnCount", "ComplexMethod")
    private fun constructDefaultInstance(type: IrType, builder: IrBuilderWithScope, depth: Int): IrExpression {
        val classSymbol = type.classOrNull ?: return createDummyInstance(type, builder)
        val irClass = classSymbol.owner

        // Don't try to stub final classes - they can't be extended
        // This typically includes external library classes like HttpClient
        if (irClass.modality == Modality.FINAL) {
            return createDummyInstance(type, builder)
        }

        // If we have a stub generator, use it for interfaces and classes we can't easily construct
        if (generateStubForClass != null && !canConstructSafely(type, depth)) {
            val stubClass = generateStubForClass.invoke(irClass)
            val stubConstructor = stubClass.constructors.first()
            return builder.irCallConstructor(stubConstructor.symbol, emptyList())
        }

        // Can't instantiate interfaces or abstract classes without stub generator
        if (irClass.kind == ClassKind.INTERFACE || irClass.kind == ClassKind.ANNOTATION_CLASS) {
            return createDummyInstance(type, builder)
        }

        // Try to find a suitable constructor (prefer constructors with fewer parameters)
        val constructors = irClass.constructors.sortedBy { constructor ->
            constructor.parameters.count { it.kind == IrParameterKind.Regular }
        }

        // Try each constructor, starting with the simplest one
        for (constructor in constructors) {
            try {
                return builder.irCallConstructor(constructor.symbol, emptyList()).apply {
                    for (param in constructor.parameters) {
                        if (param.kind == IrParameterKind.Regular) {
                            arguments[param.indexInParameters] = defaultIrValue(param.type, builder, depth + 1)
                        }
                    }
                }
            } catch (_: Exception) {
                // If this constructor doesn't work, try the next one
                continue
            }
        }

        // If no constructor worked, use dummy instance
        return createDummyInstance(type, builder)
    }

    /**
     * Creates an expression that uses the provided parameter value if non-null,
     * otherwise falls back to a default value. Equivalent to: param ?: defaultValue
     */
    fun createNullCoalescing(
        paramValue: IrExpression,
        targetType: IrType,
        builder: IrBuilderWithScope,
    ): IrExpression {
        val defaultValue = defaultIrValue(targetType, builder, depth = 0)
        // irIfNull(type, subject, thenPart, elsePart) means: if subject is null -> thenPart, else -> elsePart
        return builder.irIfNull(targetType, paramValue, defaultValue, paramValue)
    }
}
