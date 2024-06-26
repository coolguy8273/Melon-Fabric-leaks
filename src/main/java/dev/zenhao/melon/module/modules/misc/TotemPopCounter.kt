package dev.zenhao.melon.module.modules.misc

import dev.zenhao.melon.manager.EntityManager
import dev.zenhao.melon.manager.FriendManager
import dev.zenhao.melon.module.Category
import dev.zenhao.melon.module.Module
import melon.events.TickEvent
import melon.notification.NotificationManager
import melon.system.event.safeEventListener
import melon.utils.chat.ChatUtil
import net.minecraft.entity.EntityStatuses
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket
import java.util.concurrent.ConcurrentHashMap

object TotemPopCounter : Module(
    name = "TotemPopCounter",
    langName = "图腾爆炸提示",
    description = "Counts how many times players pop",
    category = Category.MISC
) {
    private var mode = msetting("Mode", Mode.Both)
    private var playerList = ConcurrentHashMap<PlayerEntity, Int>()

    enum class Mode {
        Notification, Chat, Both
    }

    init {
        onPacketReceive { event ->
            if (event.packet is EntityStatusS2CPacket) {
                val players = event.packet.getEntity(world)
                if (event.packet.status == EntityStatuses.USE_TOTEM_OF_UNDYING && players is PlayerEntity) {
                    if (playerList.containsKey(players)) {
                        playerList[players]?.let {
                            playerList[players] = it + 1
                        }
                    } else {
                        playerList[players] = 1
                    }

                    val name = players.name.string
                    val pop = playerList[players]
                    if (players.isAlive) {
                        if (FriendManager.isFriend(name) && player != players) {
                            if (mode.value == Mode.Chat || mode.value == Mode.Both) {
                                ChatUtil.sendMessage("Your Friend $name Popped ${ChatUtil.colorKANJI}$pop Totem!")
                            }
                        } else if (player == players) {
                            if (mode.value == Mode.Chat || mode.value == Mode.Both) {
                                ChatUtil.sendMessage("I Popped ${ChatUtil.colorKANJI}$pop Totem!")
                            }
                            if (mode.value == Mode.Notification || mode.value == Mode.Both) {
                                NotificationManager.addNotification("I Popped ${ChatUtil.colorKANJI}$pop Totem!")
                            }
                        } else {
                            if (mode.value == Mode.Chat || mode.value == Mode.Both) {
                                ChatUtil.sendMessage("$name Popped ${ChatUtil.colorKANJI}$pop Totem!")
                            }
                            if (mode.value == Mode.Notification || mode.value == Mode.Both) {
                                NotificationManager.addNotification("$name Popped ${ChatUtil.colorKANJI}$pop Totem!")
                            }
                        }
                    }
                }
            }
        }

        safeEventListener<TickEvent.Pre> {
            playerList.forEach {
                if (!EntityManager.players.contains(it.key)) {
                    if (mode.value == Mode.Chat || mode.value == Mode.Both) {
                        ChatUtil.sendMessage("${it.key.entityName} died after popping ${it.value} Totems!")
                    }
                    if (mode.value == Mode.Notification || mode.value == Mode.Both) {
                        NotificationManager.addNotification("${it.key.entityName} ${ChatUtil.DARK_AQUA}died after popped ${it.value} totems!", 2000)
                    }
                    playerList.remove(it.key)
                }
            }
        }
    }
}