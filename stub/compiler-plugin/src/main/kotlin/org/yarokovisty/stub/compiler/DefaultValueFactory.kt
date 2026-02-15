@file:Suppress("DEPRECATION_ERROR")

package org.yarokovisty.stub.compiler

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irLong
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
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

@OptIn(DeprecatedForRemovalCompilerApi::class, UnsafeDuringIrConstructionAPI::class)
object DefaultValueFactory {

    private const val MAX_RECURSION_DEPTH = 10

    @Suppress("MagicNumber")
    fun defaultIrValue(type: IrType, builder: IrBuilderWithScope, depth: Int = 0): IrExpression =
        when {
            type.isNullable() -> builder.irNull()
            depth >= MAX_RECURSION_DEPTH -> builder.irNull()
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

    @Suppress("ReturnCount")
    private fun constructDefaultInstance(type: IrType, builder: IrBuilderWithScope, depth: Int): IrExpression {
        val classSymbol = type.classOrNull ?: return builder.irNull()
        val irClass = classSymbol.owner
        val constructor = irClass.constructors.firstOrNull { it.isPrimary }
            ?: irClass.constructors.firstOrNull()
            ?: return builder.irNull()
        return builder.irCallConstructor(constructor.symbol, emptyList()).apply {
            for ((index, param) in constructor.valueParameters.withIndex()) {
                putValueArgument(index, defaultIrValue(param.type, builder, depth + 1))
            }
        }
    }
}
