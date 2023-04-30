package de.miraculixx.mchallenge.utils.gui.actions

import de.miraculixx.challenge.api.modules.challenges.ChallengeTags
import de.miraculixx.kpaper.items.customModel
import de.miraculixx.challenge.api.modules.challenges.Challenges
import de.miraculixx.mcore.gui.GUIEvent
import de.miraculixx.mcore.gui.InventoryUtils.get
import de.miraculixx.mcore.gui.data.CustomInventory
import de.miraculixx.mcore.gui.items.ItemFilterProvider
import de.miraculixx.mchallenge.utils.gui.GUITypes
import de.miraculixx.challenge.api.settings.challenges
import de.miraculixx.challenge.api.settings.getSetting
import de.miraculixx.mchallenge.utils.getAccountStatus
import de.miraculixx.mchallenge.utils.gui.buildInventory
import de.miraculixx.mchallenge.utils.gui.items.ItemsChallengeSettings
import de.miraculixx.mvanilla.extensions.*
import de.miraculixx.mvanilla.messages.*
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

class GUIChallenge : GUIEvent {
    private val validFilters = arrayOf(
        de.miraculixx.challenge.api.modules.challenges.ChallengeTags.NO_FILTER,
        de.miraculixx.challenge.api.modules.challenges.ChallengeTags.FUN,
        de.miraculixx.challenge.api.modules.challenges.ChallengeTags.MEDIUM,
        de.miraculixx.challenge.api.modules.challenges.ChallengeTags.HARD,
        de.miraculixx.challenge.api.modules.challenges.ChallengeTags.FREE,
        de.miraculixx.challenge.api.modules.challenges.ChallengeTags.FORCE,
        de.miraculixx.challenge.api.modules.challenges.ChallengeTags.RANDOMIZER,
        de.miraculixx.challenge.api.modules.challenges.ChallengeTags.MULTIPLAYER
    )

    override val run: (InventoryClickEvent, CustomInventory) -> Unit = event@{ it: InventoryClickEvent, inv: CustomInventory ->
        it.isCancelled = true
        val player = it.whoClicked as? Player ?: return@event
        val item = it.currentItem ?: return@event
        val meta = item.itemMeta
        val click = it.click

        when (meta?.customModel ?: 0) {
            0 -> {
                if (it.inventory.size == 9*6) {
                    GUITypes.CHALLENGE_MENU.buildInventory(player, "${player.uniqueId}-CHALLENGES", inv.itemProvider, this)
                    player.click()
                }
            }

            9005 -> {
                val current = meta.persistentDataContainer.get(NamespacedKey(namespace, "gui.storage.filter")) ?: return@event
                val currentFilter = enumOf<de.miraculixx.challenge.api.modules.challenges.ChallengeTags>(current) ?: return@event
                val newFilter = validFilters.enumRotate(currentFilter)
                (inv.itemProvider as? ItemFilterProvider)?.filter = newFilter.name
                player.click()
                inv.update()
            }

            else -> {
                val challengeKey = meta?.persistentDataContainer.get(NamespacedKey(namespace, "gui.challenge")) ?: return@event
                val challenge = enumOf<de.miraculixx.challenge.api.modules.challenges.Challenges>(challengeKey) ?: return@event

                if (!getAccountStatus() && !challenge.filter.contains(de.miraculixx.challenge.api.modules.challenges.ChallengeTags.FREE)) {
                    //NO PERMS TODO
                    player.soundError()
                    player.closeInventory()
                    player.sendMessage(prefix + cmp("No MUtils account connected!", cError))
                    if (de.miraculixx.mchallenge.MChallenge.bridgeAPI == null) player.sendMessage(prefix + cmp("Use /ch bridge-install to use all account features!", cSuccess))
                    else player.sendMessage(prefix + cmp("Use /login to connect your account", cSuccess))
                    return@event
                }

                val data = challenges.getSetting(challenge)
                if (click.isLeftClick) {
                    data.active = data.active.toggle(player)
                    inv.update()
                } else if (click.isRightClick) {
                    val settings = data.settings
                    if (settings.isEmpty()) {
                        player.soundStone()
                        return@event
                    }
                    GUITypes.CHALLENGE_SETTINGS.buildInventory(player, "CH-$challengeKey", ItemsChallengeSettings(settings, challenge), GUIChallengeSettings(inv, null))
                    player.click()
                }
            }
        }
    }
}
