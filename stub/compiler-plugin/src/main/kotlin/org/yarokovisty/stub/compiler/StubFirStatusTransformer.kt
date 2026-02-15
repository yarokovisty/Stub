@file:Suppress("DEPRECATION_ERROR")

package org.yarokovisty.stub.compiler

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

class StubFirStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {

    override fun needTransformStatus(declaration: FirDeclaration): Boolean =
        declaration is FirRegularClass || declaration is FirSimpleFunction || declaration is FirProperty

    override fun transformStatus(
        status: FirDeclarationStatus,
        regularClass: FirRegularClass,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean,
    ): FirDeclarationStatus =
        openStatus(status)

    override fun transformStatus(
        status: FirDeclarationStatus,
        function: FirSimpleFunction,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean,
    ): FirDeclarationStatus =
        openStatus(status)

    override fun transformStatus(
        status: FirDeclarationStatus,
        property: FirProperty,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean,
    ): FirDeclarationStatus =
        openStatus(status)

    @Suppress("ReturnCount")
    private fun openStatus(status: FirDeclarationStatus): FirDeclarationStatus {
        if (status !is FirDeclarationStatusImpl) return status
        if (status.modality == Modality.ABSTRACT || status.modality == Modality.OPEN) return status
        val newStatus = FirDeclarationStatusImpl(status.visibility, Modality.OPEN)
        copyFlags(status, newStatus)
        return newStatus
    }

    private fun copyFlags(from: FirDeclarationStatusImpl, to: FirDeclarationStatusImpl) {
        to.isExpect = from.isExpect
        to.isActual = from.isActual
        to.isOverride = from.isOverride
        to.isOperator = from.isOperator
        to.isInfix = from.isInfix
        to.isInline = from.isInline
        to.isTailRec = from.isTailRec
        to.isExternal = from.isExternal
        to.isConst = from.isConst
        to.isLateInit = from.isLateInit
        to.isInner = from.isInner
        to.isCompanion = from.isCompanion
        to.isData = from.isData
        to.isSuspend = from.isSuspend
        to.isStatic = from.isStatic
        to.isFromSealedClass = from.isFromSealedClass
        to.isFromEnumClass = from.isFromEnumClass
        to.isFun = from.isFun
        to.hasStableParameterNames = from.hasStableParameterNames
    }
}
