@file:Suppress("TooManyFunctions")

package org.yarokovisty.stub.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
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
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isDouble
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.classIdOrFail
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
@OptIn(UnsafeDuringIrConstructionAPI::class)
class CreateStubTransformer(
    private val pluginContext: IrPluginContext,
) : IrElementTransformerVoid() {

    private val generatedStubs = mutableMapOf<String, IrClass>()
    private val defaultValueFactory = DefaultValueFactory(pluginContext, ::generateStubClass)

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
            val regularParams = symbol.owner.parameters.filter { it.kind == IrParameterKind.Regular }
            regularParams.size == 1 && regularParams[0].varargElementType != null
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

        val typeArg = expression.typeArguments[0]
            ?: return super.visitCall(expression)

        val targetClass = typeArg.classOrNull?.owner
            ?: return super.visitCall(expression)

        val fqName = targetClass.classIdOrFail.asFqNameString()

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
            FunctionCollector.collectAbstractFunctions(targetClass)
        } else {
            FunctionCollector.collectOverridableFunctions(targetClass)
        }
        methods.forEach { addOverrideMethod(stubClass, it, delegateField) }

        // Handle properties separately to avoid illegal <get-X> method names
        if (!isInterface) {
            val properties = FunctionCollector.collectOverridableProperties(targetClass)
            properties.forEach { addOverrideProperty(stubClass, it, delegateField) }
        }

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

    @Suppress("ComplexMethod", "LongMethod")
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
            fun canProvideDefaultValue(type: IrType): Boolean {
                if (type.isNullable()) return true
                val classSymbol = type.classOrNull ?: return false
                val irClass = classSymbol.owner
                // Can provide default for non-final classes (we can stub them) and primitives
                return irClass.modality != Modality.FINAL ||
                       type.isInt() || type.isLong() || type.isFloat() ||
                       type.isDouble() || type.isBoolean() || type.isString()
            }

            // Check if we can safely call the parent constructor before adding parameters
            // We can call it if all parameters can be constructed (primitives, stubs, etc.)
            // but not if they're final external classes
            val canSafelyCallParent = if (!isInterface) {
                val targetConstructor = targetClass.constructors.firstOrNull { it.isPrimary }
                    ?: targetClass.constructors.first()
                targetConstructor.parameters
                    .filter { it.kind == IrParameterKind.Regular }
                    .all { param -> canProvideDefaultValue(param.type) }
            } else {
                true
            }

            val constructorParams = if (!isInterface && canSafelyCallParent) {
                val targetConstructor = targetClass.constructors.firstOrNull { it.isPrimary }
                    ?: targetClass.constructors.first()

                // Add constructor parameters with nullable types and default null values
                targetConstructor.parameters
                    .filter { it.kind == IrParameterKind.Regular }
                    .map { param ->
                        addValueParameter {
                            name = param.name
                            type = param.type.makeNullable()
                            origin = IrDeclarationOrigin.DEFINED
                        }.also {
                            it.defaultValue = DeclarationIrBuilder(pluginContext, symbol).run {
                                irExprBody(irNull())
                            }
                        }
                    }
            } else {
                emptyList()
            }

            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                if (isInterface) {
                    +irDelegatingConstructorCall(
                        pluginContext.irBuiltIns.anyClass.owner.constructors.first(),
                    )
                } else {
                    val targetConstructor = targetClass.constructors.firstOrNull { it.isPrimary }
                        ?: targetClass.constructors.first()

                    if (canSafelyCallParent) {
                        +irDelegatingConstructorCall(targetConstructor).apply {
                            constructorParams.forEachIndexed { index, stubParam ->
                                val targetParam = targetConstructor.parameters
                                    .filter { it.kind == IrParameterKind.Regular }[index]

                                // Use the provided parameter value, or generate a default if null
                                val paramValue = irGet(stubParam)
                                arguments[targetParam.indexInParameters] =
                                    defaultValueFactory.createNullCoalescing(
                                        paramValue,
                                        targetParam.type,
                                        this@irBlockBody,
                                    )
                            }
                        }
                    } else {
                        // Can't safely call parent constructor - call Any() instead
                        // This is safe because all method calls are intercepted by the stub delegate
                        +irDelegatingConstructorCall(
                            pluginContext.irBuiltIns.anyClass.owner.constructors.first(),
                        )
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
                stubClass.thisReceiver?.copyTo(this)?.let { dispatchParam ->
                    parameters = listOf(dispatchParam) + parameters
                }
                overriddenSymbols = listOf(stubbableProp.getter!!.symbol)

                body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                    +irReturn(irGetField(irGet(dispatchReceiverParameter!!), delegateField))
                }
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
            stubClass.thisReceiver?.copyTo(this)?.let { dispatchParam ->
                parameters = listOf(dispatchParam) + parameters
            }
            overriddenSymbols = listOf(function.symbol)

            val addedParams = function.parameters
                .filter { it.kind == IrParameterKind.Regular }
                .map { param -> addValueParameter(param.name.asString(), param.type) }

            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                val methodName = extractMethodName(function)
                val delegateGet = irGetField(irGet(dispatchReceiverParameter!!), delegateField)
                val handleRegularParams = handleFunction.parameters
                    .filter { it.kind == IrParameterKind.Regular }
                val handleCall = irCall(handleFunction).apply {
                    dispatchReceiver = delegateGet
                    typeArguments[0] = function.returnType
                    arguments[handleRegularParams[0].indexInParameters] = irString(methodName)

                    if (addedParams.isNotEmpty()) {
                        val listOfRegularParams = listOfFunction.owner.parameters
                            .filter { it.kind == IrParameterKind.Regular }
                        val argsList = irCall(listOfFunction).apply {
                            typeArguments[0] = anyNullableType
                            arguments[listOfRegularParams[0].indexInParameters] = irVararg(
                                anyNullableType,
                                addedParams.map { irGet(it) },
                            )
                        }
                        arguments[handleRegularParams[1].indexInParameters] = argsList
                    }
                }
                +irReturn(handleCall)
            }
        }
    }

    private fun addOverrideProperty(stubClass: IrClass, property: IrProperty, delegateField: IrField) {
        stubClass.addProperty {
            name = property.name
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.OPEN
            origin = IrDeclarationOrigin.DEFINED
        }.apply {
            overriddenSymbols = listOf(property.symbol)
            property.getter?.let { originalGetter ->
                addGetter {
                    returnType = originalGetter.returnType
                    visibility = DescriptorVisibilities.PUBLIC
                    modality = Modality.OPEN
                    origin = IrDeclarationOrigin.DEFINED
                }.apply {
                    stubClass.thisReceiver?.copyTo(this)?.let { dispatchParam ->
                        parameters = listOf(dispatchParam) + parameters
                    }
                    overriddenSymbols = listOf(originalGetter.symbol)

                    body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                        val methodName = property.name.asString()
                        val delegateGet = irGetField(irGet(dispatchReceiverParameter!!), delegateField)
                        val handleRegularParams = handleFunction.parameters
                            .filter { it.kind == IrParameterKind.Regular }
                        val handleCall = irCall(handleFunction).apply {
                            dispatchReceiver = delegateGet
                            typeArguments[0] = originalGetter.returnType
                            arguments[handleRegularParams[0].indexInParameters] = irString(methodName)
                        }
                        +irReturn(handleCall)
                    }
                }
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
