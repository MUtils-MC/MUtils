package de.miraculixx.mchallenge.commands

import de.miraculixx.challenge.api.modules.challenges.ChallengeStatus
import de.miraculixx.challenge.api.modules.challenges.ChallengeTags
import de.miraculixx.kpaper.extensions.broadcast
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.mchallenge.MChallenge
import de.miraculixx.mchallenge.gui.GUITypes
import de.miraculixx.mchallenge.gui.actions.ActionChallenge
import de.miraculixx.mchallenge.gui.actions.ActionMenu
import de.miraculixx.mchallenge.gui.buildInventory
import de.miraculixx.mchallenge.gui.items.ItemsChallenge
import de.miraculixx.mchallenge.gui.items.ItemsMenu
import de.miraculixx.mchallenge.modules.ChallengeManager
import de.miraculixx.mchallenge.utils.UniversalChallenge
import de.miraculixx.mchallenge.utils.config.ConfigManager
import de.miraculixx.mchallenge.utils.getAccountStatus
import de.miraculixx.mvanilla.extensions.soundEnable
import de.miraculixx.mvanilla.extensions.soundError
import de.miraculixx.mvanilla.messages.*
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.kotlindsl.*
import org.bukkit.Sound

class ChallengeCommand {
    private var apiCooldown = false

    private val command = commandTree("challenge") {
        withPermission("command.challenge")
        withAliases("ch")
        playerExecutor { player, _ ->
            GUITypes.MAIN_MENU.buildInventory(player, "GLOBAL_MENU", ItemsMenu(), ActionMenu())
            player.playSound(player, Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1f)
        }

        literalArgument("addons") {
            playerExecutor { player, _ ->
                if (ChallengeManager.getCustomChallenges().isEmpty()) {
                    player.sendMessage(prefix + msg("command.noAddons"))
                    player.soundError()
                    return@playerExecutor
                }
                GUITypes.CHALLENGE_MENU.buildInventory(
                    player,
                    player.uniqueId.toString(),
                    ItemsChallenge(ChallengeManager.getCustomChallenges().map { ch -> UniversalChallenge(addon = ch.key) }.toSet()),
                    ActionChallenge(null)
                )
                player.playSound(player, Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1f)
            }
        }

        literalArgument("stop") {
            anyExecutor { sender, _ ->
                if (ChallengeManager.stopChallenges()) {
                    ChallengeManager.status = ChallengeStatus.STOPPED
                    broadcast(prefix + msg("command.challenge.stop", listOf(sender.name)))
                } else sender.sendMessage(prefix + msg("command.challenge.alreadyOff"))
            }
        }

        literalArgument("start") {
            anyExecutor { sender, _ ->
                if (ChallengeManager.status == ChallengeStatus.RUNNING) {
                    sender.sendMessage(prefix + msg("command.challenge.alreadyOn"))
                } else if (ChallengeManager.startChallenges()) {
                    ChallengeManager.status = ChallengeStatus.RUNNING
                    broadcast(prefix + msg("command.challenge.start", listOf(sender.name)))
                } else sender.sendMessage(prefix + msg("command.challenge.failed"))
            }
        }

        literalArgument("pause") {
            anyExecutor { sender, _ ->
                if (ChallengeManager.pauseChallenges()) {
                    ChallengeManager.status = ChallengeStatus.PAUSED
                    broadcast(prefix + msg("command.challenge.pause", listOf(sender.name)))
                } else sender.sendMessage(prefix + msg("command.challenge.alreadyOff"))
            }
        }

        literalArgument("resume") {
            anyExecutor { sender, _ ->
                if (ChallengeManager.resumeChallenges()) {
                    ChallengeManager.status = ChallengeStatus.RUNNING
                    broadcast(prefix + msg("command.challenge.continue", listOf(sender.name)))
                } else sender.sendMessage(prefix + msg("command.challenge.alreadyOff"))
            }
        }

        argument(LiteralArgument("login").withPermission("mutils.command.login")) {
            anyExecutor { sender, _ -> sender.sendMessage(prefix + cmp("Please provide a valid key", cError)) }
            stringArgument("key") {
                playerExecutor { player, args ->
                    val key = args[0] as String

                    if (getAccountStatus()) {
                        player.sendMessage(
                            prefix + cmp(
                                "You are already logged in. To disconnect this server, remove it from your account dashboard",
                                cError
                            )
                        )
                        player.soundError()
                        return@playerExecutor
                    }

                    if (apiCooldown) {
                        player.sendMessage(prefix + cmp("Please wait a bit between logins", cError))
                        player.soundError()
                        return@playerExecutor
                    }
                    apiCooldown = true
                    taskRunLater(20 * 3) { apiCooldown = false }

                    player.sendMessage(prefix + cmp("Trying to log in..."))
                    MChallenge.bridgeAPI.activate(player.uniqueId, key) { success: Boolean, message: String ->
                        if (success) {
                            player.sendMessage(prefix + cmp("Successfully logged in your account!", cSuccess))
                            player.sendMessage(prefix + cmp("Please perform a server restart in near future"))
                            MChallenge.bridgeAPI.saveData(autoUpdate = true)
                            player.soundEnable()
                        } else {
                            player.sendMessage(prefix + cmp("Failed to login!", cError))
                            player.sendMessage(prefix + cmp(message, cError))
                            player.soundError()
                        }
                    }
                }
                anyExecutor { sender, _ ->
                    sender.sendMessage(prefix + cmp("Please execute this command as a player", cError))
                }
            }
        }

        argument(LiteralArgument("settings").withPermission("mutils.command.settings")) {
            literalArgument("debug") {
                booleanArgument("active") {
                    anyExecutor { sender, args ->
                        val active = args[0] as Boolean
                        debug = active
                        ConfigManager.settings.debug = active
                        sender.sendMessage(prefix + msg("command.debug", listOf(active.toString())))
                    }
                }
            }
            literalArgument("language") {
                stringArgument("lang") {
                    anyExecutor { sender, args ->
                        val key = args[0] as String
                        if (ConfigManager.localization.setLanguage(key)) {
                            ConfigManager.settings.language = key
                            sender.sendMessage(prefix + msg("command.lang.success", listOf(key)))
                        } else sender.sendMessage(prefix + msg("command.lang.fail", listOf(key)))
                    }
                }
            }
        }
    }
}