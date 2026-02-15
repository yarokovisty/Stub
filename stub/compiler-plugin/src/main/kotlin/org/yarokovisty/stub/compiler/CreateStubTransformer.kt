@file:Suppress("TooManyFunctions", "DEPRECATION_ERROR")

package org.yarokovisty.stub.compiler

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irLong
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isDouble
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Suppress("ReturnCount")
@OptIn(DeprecatedForRemovalCompilerApi::class, UnsafeDuringIrConstructionAPI::class)
class CreateStubTransformer(
    private val pluginContext: IrPluginContext,
) : IrElementTransformerVoid() {

    private val generatedStubs = mutableMapOf<String, IrClass>()

    private val stubDelegateClassId = ClassId(
        FqName("org.yarokovisty.stub.runtime"),
        Name.identifier("StubDelegate"),
    )

    private val stubbableClassId = ClassId(
        FqName("org.yarokovisty.stub.runtime"),
        Name.identifier("Stubbable"),
    )

    private val stubDelegateClass: IrClassSymbol by lazy {
        pluginContext.referenceClass(stubDelegateClassId)
            ?: error("Cannot find StubDelegate class")
    }

    private val stubbableClass: IrClassSymbol by lazy {
        pluginContext.referenceClass(stubbableClassId)
            ?: error("Cannot find Stubbable class")
    }

    private val handleFunction: IrSimpleFunction by lazy {
        stubDelegateClass.owner.functions
            .first { it.name == Name.identifier("handle") }
    }

    private val listOfFunction by lazy {
        pluginContext.referenceFunctions(
            CallableId(FqName("kotlin.collections"), Name.identifier("listOf")),
        ).first { symbol ->
            val params = symbol.owner.valueParameters
            params.size == 1 && params[0].varargElementType != null
        }
    }

    private val anyNullableType: IrType by lazy {
        pluginContext.irBuiltIns.anyNType
    }

    private var currentFile: IrFile? = null

    override fun visitFile(declaration: IrFile): IrFile {
        currentFile = declaration
        val result = super.visitFile(declaration)
        currentFile = null
        return result
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.owner

        if (!isStubCall(callee)) {
            return super.visitCall(expression)
        }

        val typeArg = expression.getTypeArgument(0)
            ?: return super.visitCall(expression)

        val targetClass = typeArg.classOrNull?.owner
            ?: return super.visitCall(expression)

        val fqName = targetClass.defaultType.classFqName?.asString()
            ?: return super.visitCall(expression)

        val stubClass = generatedStubs.getOrPut(fqName) {
            generateStubClass(targetClass)
        }

        val constructor = stubClass.constructors.first()
        return DeclarationIrBuilder(pluginContext, expression.symbol).irCallConstructor(
            constructor.symbol,
            emptyList(),
        ).apply {
            type = targetClass.defaultType
        }
    }

    private fun isStubCall(callee: IrSimpleFunction): Boolean {
        if (callee.name != Name.identifier("stub") &&
            callee.name != Name.identifier("createStub")
        ) {
            return false
        }
        val parent = callee.parent
        if (parent !is IrPackageFragment) return false
        val pkg = parent.packageFqName.asString()
        return pkg == "org.yarokovisty.stub.dsl"
    }

    private fun generateStubClass(targetClass: IrClass): IrClass {
        val isInterface = targetClass.kind == ClassKind.INTERFACE

        val stubClassName = "Stub__${targetClass.name.asString()}"

        val stubClass = pluginContext.irFactory.buildClass {
            name = Name.identifier(stubClassName)
            kind = ClassKind.CLASS
            modality = Modality.FINAL
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            superTypes = listOf(targetClass.defaultType, stubbableClass.owner.defaultType)
            val file = currentFile
            if (file != null) {
                parent = file
            }
            createThisReceiverParameter()
        }

        val delegateField = addDelegateField(stubClass)
        addConstructor(stubClass, delegateField, targetClass, isInterface)
        addStubbableGetter(stubClass, delegateField)

        val methods = if (isInterface) {
            collectAbstractFunctions(targetClass)
        } else {
            collectOverridableFunctions(targetClass)
        }
        methods.forEach { addOverrideMethod(stubClass, it, delegateField) }

        currentFile?.declarations?.add(stubClass)

        return stubClass
    }

    private fun addDelegateField(stubClass: IrClass): IrField =
        stubClass.addField {
            name = Name.identifier("stubDelegate\$impl")
            type = stubDelegateClass.defaultType
            visibility = DescriptorVisibilities.PRIVATE
            origin = IrDeclarationOrigin.DEFINED
        }

    private fun addConstructor(
        stubClass: IrClass,
        delegateField: IrField,
        targetClass: IrClass,
        isInterface: Boolean,
    ) {
        stubClass.addConstructor {
            isPrimary = true
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                if (isInterface) {
                    +irDelegatingConstructorCall(
                        pluginContext.irBuiltIns.anyClass.owner.constructors.first(),
                    )
                } else {
                    val targetConstructor = targetClass.constructors.firstOrNull { it.isPrimary }
                        ?: targetClass.constructors.first()
                    +irDelegatingConstructorCall(targetConstructor).apply {
                        for ((index, param) in targetConstructor.valueParameters.withIndex()) {
                            putValueArgument(index, defaultIrValue(param.type, this@irBlockBody))
                        }
                    }
                }
                +irSetField(
                    irGet(stubClass.thisReceiver!!),
                    delegateField,
                    irCallConstructor(stubDelegateClass.owner.constructors.first().symbol, emptyList()),
                )
            }
        }
    }

    private fun addStubbableGetter(stubClass: IrClass, delegateField: IrField) {
        val stubbableProp = stubbableClass.owner.declarations
            .filterIsInstance<IrProperty>()
            .first { it.name == Name.identifier("stubDelegate") }

        stubClass.addProperty {
            name = Name.identifier("stubDelegate")
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.OPEN
            origin = IrDeclarationOrigin.DEFINED
        }.apply {
            overriddenSymbols = listOf(stubbableProp.symbol)

            addGetter {
                returnType = stubDelegateClass.defaultType
                visibility = DescriptorVisibilities.PUBLIC
                modality = Modality.OPEN
                origin = IrDeclarationOrigin.DEFINED
            }.apply {
                dispatchReceiverParameter = stubClass.thisReceiver?.copyTo(this)
                overriddenSymbols = listOf(stubbableProp.getter!!.symbol)

                body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                    +irReturn(irGetField(irGet(dispatchReceiverParameter!!), delegateField))
                }
            }
        }
    }

    private fun collectOverridableFunctions(irClass: IrClass): List<IrSimpleFunction> {
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

    private fun isOverridableFunction(function: IrSimpleFunction): Boolean =
        function.visibility == DescriptorVisibilities.PUBLIC &&
            !function.isFakeOverride &&
            function.name != Name.identifier("<init>")

    private fun collectAbstractFunctions(irClass: IrClass): List<IrSimpleFunction> {
        val result = mutableListOf<IrSimpleFunction>()
        addAbstractDeclarations(irClass, result)
        addSuperInterfaceFunctions(irClass, result)
        return result.distinctBy { it.name.asString() }
    }

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

    @Suppress("LongMethod")
    private fun addOverrideMethod(stubClass: IrClass, function: IrSimpleFunction, delegateField: IrField) {
        stubClass.addFunction {
            name = function.name
            returnType = function.returnType
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.OPEN
             isSuspend = function.isSuspend
            origin = IrDeclarationOrigin.DEFINED
        }.apply {
            dispatchReceiverParameter = stubClass.thisReceiver?.copyTo(this)
            overriddenSymbols = listOf(function.symbol)

            val addedParams = function.valueParameters.map { param ->
                addValueParameter(param.name.asString(), param.type)
            }

            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                val methodName = extractMethodName(function)
                val delegateGet = irGetField(irGet(dispatchReceiverParameter!!), delegateField)
                val handleCall = irCall(handleFunction).apply {
                    dispatchReceiver = delegateGet
                    putTypeArgument(0, function.returnType)
                    putValueArgument(0, irString(methodName))

                    if (addedParams.isNotEmpty()) {
                        val argsList = irCall(listOfFunction).apply {
                            putTypeArgument(0, anyNullableType)
                            putValueArgument(
                                0,
                                irVararg(
                                    anyNullableType,
                                    addedParams.map { irGet(it) },
                                ),
                            )
                        }
                        putValueArgument(1, argsList)
                    }
                }
                +irReturn(handleCall)
            }
        }
    }

    @Suppress("MagicNumber")
    private fun defaultIrValue(type: IrType, builder: IrBuilderWithScope): IrExpression =
        when {
            type.isNullable() -> builder.irNull()
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
            else -> constructDefaultInstance(type, builder)
        }

    private fun constructDefaultInstance(type: IrType, builder: IrBuilderWithScope): IrExpression {
        val classSymbol = type.classOrNull ?: return builder.irNull()
        val irClass = classSymbol.owner
        val constructor = irClass.constructors.firstOrNull { it.isPrimary }
            ?: irClass.constructors.firstOrNull()
            ?: return builder.irNull()
        return builder.irCallConstructor(constructor.symbol, emptyList()).apply {
            for ((index, param) in constructor.valueParameters.withIndex()) {
                putValueArgument(index, defaultIrValue(param.type, builder))
            }
        }
    }

    private fun extractMethodName(function: IrSimpleFunction): String {
        val name = function.name.asString()
        return when {
            name.startsWith("<get-") -> name.removePrefix("<get-").removeSuffix(">")
            name.startsWith("<set-") -> name.removePrefix("<set-").removeSuffix(">")
            else -> name
        }
    }
}
