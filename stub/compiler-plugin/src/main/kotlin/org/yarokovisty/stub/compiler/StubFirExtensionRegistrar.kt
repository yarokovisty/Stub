package org.yarokovisty.stub.compiler

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class StubFirExtensionRegistrar : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +::StubFirStatusTransformer
    }
}
