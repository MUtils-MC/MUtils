package de.miraculixx.mchallenge.modules

import de.miraculixx.challenge.api.MChallengeAPI
import de.miraculixx.challenge.api.modules.challenges.*
import de.miraculixx.challenge.api.settings.ChallengeData
import de.miraculixx.kpaper.extensions.console
import de.miraculixx.mchallenge.MChallenge
import de.miraculixx.mchallenge.modules.challenges.challenges
import de.miraculixx.mchallenge.modules.challenges.Challenges
import de.miraculixx.mchallenge.modules.challenges.StatusChanger
import de.miraculixx.mchallenge.utils.UniversalChallenge
import de.miraculixx.mchallenge.utils.config.Configurable
import de.miraculixx.mvanilla.extensions.readJsonString
import de.miraculixx.mvanilla.messages.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.*

object ChallengeManager : MChallengeAPI(), Configurable {
    init {
        instance = this
    }

    private val fileFavorites = File(MChallenge.configFolder, "favorites.json")
    private val fileHistory = File(MChallenge.configFolder, "history.json")
    private val fileSettings = File("${MChallenge.configFolder.path}/settings.json")

    private val statusChanger = StatusChanger()
    var status = ChallengeStatus.STOPPED
    private val activatedChallenges: MutableList<Challenge> = mutableListOf()
    val favoriteChallenges: MutableSet<UniversalChallenge> = mutableSetOf()
    val historyChallenges: MutableList<UniversalChallenge> = mutableListOf()

    private val customChallengeMap: MutableMap<UUID, CustomChallengeData> = mutableMapOf()

    override fun getChallenges(): List<Challenge> {
        return activatedChallenges.toList()
    }

    override fun startChallenges(): Boolean {
        if (status == ChallengeStatus.RUNNING) return false
        activatedChallenges.clear()
        activatedChallenges.addAll(statusChanger.startChallenges() ?: return false)
        status = ChallengeStatus.RUNNING
        return true
    }

    override fun stopChallenges(): Boolean {
        if (status == ChallengeStatus.STOPPED) return false
        statusChanger.stopChallenges(activatedChallenges)
        status = ChallengeStatus.STOPPED
        return true
    }

    override fun pauseChallenges(): Boolean {
        if (status != ChallengeStatus.RUNNING) return false
        statusChanger.unregisterChallenges(activatedChallenges)
        status = ChallengeStatus.PAUSED
        return true
    }

    override fun resumeChallenges(): Boolean {
        if (status != ChallengeStatus.PAUSED) return false
        statusChanger.registerChallenges(activatedChallenges)
        status = ChallengeStatus.RUNNING
        return true
    }

    override fun getChallengeStatus(): ChallengeStatus {
        return status
    }

    override fun addChallenge(key: UUID, data: CustomChallengeData): CustomChallengeData? {
        if (customChallengeMap.containsKey(key)) return null
        val finalData = data.copy(tags = data.tags.plus(ChallengeTags.ADDON))
        customChallengeMap[key] = finalData
        return finalData
    }

    override fun getChallenge(key: UUID): CustomChallengeData? {
        return customChallengeMap[key]
    }

    override fun removeChallenge(key: UUID): Boolean {
        return customChallengeMap.remove(key) != null
    }

    fun getCustomChallenges() = customChallengeMap

    fun getCustomChallenge(uuid: UUID) = customChallengeMap[uuid]

    /**
     * Unregister and stop all activated challenges. Mostly used on reloads or server shutdowns.
     *
     * This function should no used for runtime stopping, no attempt will be made to save data. Use [stopChallenges] and [pauseChallenges] instead!
     */
    fun shutDown() {
        activatedChallenges.forEach {
            if (status != ChallengeStatus.STOPPED) it.stop()
            it.unregister()
        }
    }

    /**
     * Load in all saved data like challenge status and settings
     * @param file The save file
     */
    override fun load() {
        try {
            val data = json.decodeFromString<Map<Challenges, ChallengeData>>(fileSettings.readJsonString(true))
            data.forEach { (ch, data) ->
                challenges[ch] = data
            }
            favoriteChallenges.addAll(json.decodeFromString(fileFavorites.readJsonString(false)))
            historyChallenges.addAll(json.decodeFromString(fileHistory.readJsonString(false)))
        } catch (_: Exception) {
            console.sendMessage(challengePrefix + cmp("Failed to load configuration! This could be due to skipping some updates or manual editing.", cError))
        }
    }

    /**
     * Saves all data like challenge status and settings. Note: Only **changes** are saved! Default values will be not present in the file.
     * @param configFile The save file
     */
    override fun save() {
        if (!fileSettings.exists()) fileSettings.parentFile.mkdirs()
        fileSettings.writeText(json.encodeToString(challenges))
        fileFavorites.writeText(json.encodeToString(favoriteChallenges))
        fileHistory.writeText(json.encodeToString(historyChallenges))
    }
}