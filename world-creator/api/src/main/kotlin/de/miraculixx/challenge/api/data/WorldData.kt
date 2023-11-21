package de.miraculixx.challenge.api.data

import de.miraculixx.challenge.api.data.enums.BiomeAlgorithm
import de.miraculixx.challenge.api.data.enums.Dimension
import de.miraculixx.challenge.api.data.enums.VanillaGenerator
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Custom World Data
 *
 * From this object new worlds will be generated and saved.
 * @param presetName Name that will be visual in global library
 * @param worldName World Name
 * @param category Custom category for separate play data. Vanilla is synced with the three default worlds
 * @param seed World Seed
 * @param environment World Dimension
 * @param worldType Vanilla generation modifier - Like flat or amplified
 * @param biomeProvider Custom biome provider
 * @param chunkProviders Custom noise providers. Render order is -> Vanilla - Entry 1 - Entry 2 - ...
 * @param chunkDefaults Vanilla noise modifications
 * @param customGameRules Custom game rule map - Defaults to default values
 */
@Serializable
data class WorldData(
    var presetName: String = "MUtils",
    var worldName: String = "error",
    var category: String = "Vanilla",
    var seed: Long? = null,
    var environment: Dimension = Dimension.NORMAL,
    var worldType: VanillaGenerator = VanillaGenerator.NORMAL,
    val biomeProvider: BiomeProviderData = BiomeProviderData(BiomeAlgorithm.VANILLA, GeneratorData()),
    val chunkProviders: MutableList<GeneratorProviderData> = mutableListOf(),
    val chunkDefaults: GeneratorDefaults = GeneratorDefaults(),
    val customGameRules: MutableMap<CustomGameRule, @Contextual Any> = buildMap { CustomGameRule.entries.forEach { put(it, it.default) } }.toMutableMap()
)
