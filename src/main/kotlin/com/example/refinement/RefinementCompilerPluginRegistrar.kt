package com.example.refinement

import com.example.refinement.RefinementConfigurationKeys.REFINEMENT_ANNOTATIONS
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

object RefinementConfigurationKeys {
    val REFINEMENT_ANNOTATIONS = CompilerConfigurationKey.create<List<String>>(
        "fully qualified names of refinement annotations"
    )
}

class RefinementPluginOptions: CommandLineProcessor {
    override val pluginId: String
        get() = "org.example.refinement"

    override val pluginOptions: Collection<AbstractCliOption>
        get() = listOf(REFINEMENT_ANNOTATIONS_OPTION)

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) = when (option) {
        REFINEMENT_ANNOTATIONS_OPTION -> configuration.appendList(
            REFINEMENT_ANNOTATIONS, value
        )
        else -> error("Unexpected config option ${option.optionName}")
    }

    companion object {
        val REFINEMENT_ANNOTATIONS_OPTION = CliOption(
            optionName = "refinementAnnotations",
            valueDescription = "<fqname>",
            description = "fully qualified names of refinement annotations",
            required = false,
            allowMultipleOccurrences = true,
        )
    }
}

class RefinementCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        val annotations = configuration.get(REFINEMENT_ANNOTATIONS).orEmpty()
        FirExtensionRegistrarAdapter.registerExtension(
            FirRefinementExtensionRegistrar(annotations)
        )
    }
}