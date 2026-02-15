@file:Suppress("DEPRECATION_ERROR")

package org.yarokovisty.stub.compiler

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.name.Name

@OptIn(DeprecatedForRemovalCompilerApi::class, UnsafeDuringIrConstructionAPI::class)
object FunctionCollector {

    fun collectOverridableFunctions(irClass: IrClass): List<IrSimpleFunction> {
        val result = mutableListOf<IrSimpleFunction>()
        for (declaration in irClass.declarations) {
            if (declaration is IrSimpleFunction && isOverridableFunction(declaration)) {
                result.add(declaration)
            }
            if (declaration is IrProperty) {
                declaration.getter?.takeIf {
                    it.visibility == DescriptorVisibilities.PUBLIC &&
                        !it.isFakeOverride
                }?.let(result::add)
                declaration.setter?.takeIf {
                    it.visibility == DescriptorVisibilities.PUBLIC &&
                        !it.isFakeOverride
                }?.let(result::add)
            }
        }
        return result
    }

    fun collectAbstractFunctions(irClass: IrClass): List<IrSimpleFunction> {
        val result = mutableListOf<IrSimpleFunction>()
        addAbstractDeclarations(irClass, result)
        addSuperInterfaceFunctions(irClass, result)
        return result.distinctBy { functionSignature(it) }
    }

    private fun isOverridableFunction(function: IrSimpleFunction): Boolean =
        function.visibility == DescriptorVisibilities.PUBLIC &&
            !function.isFakeOverride &&
            function.name != Name.identifier("<init>")

    private fun addAbstractDeclarations(irClass: IrClass, result: MutableList<IrSimpleFunction>) {
        for (declaration in irClass.declarations) {
            if (declaration is IrSimpleFunction && declaration.modality == Modality.ABSTRACT) {
                result.add(declaration)
            }
            if (declaration is IrProperty) {
                declaration.getter?.takeIf { it.modality == Modality.ABSTRACT }?.let(result::add)
                declaration.setter?.takeIf { it.modality == Modality.ABSTRACT }?.let(result::add)
            }
        }
    }

    private fun addSuperInterfaceFunctions(irClass: IrClass, result: MutableList<IrSimpleFunction>) {
        for (superType in irClass.superTypes) {
            val superClass = superType.classOrNull?.owner ?: continue
            if (superClass.kind == ClassKind.INTERFACE) {
                result.addAll(collectAbstractFunctions(superClass))
            }
        }
    }

    private fun functionSignature(function: IrSimpleFunction): String =
        "${function.name}(${function.valueParameters.joinToString(",") { p ->
            p.type.classFqName?.asString() ?: "?"
        }})"
}
