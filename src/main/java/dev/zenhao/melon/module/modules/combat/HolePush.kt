package dev.zenhao.melon.module.modules.combat

import dev.zenhao.melon.manager.HotbarManager.spoofHotbar
import dev.zenhao.melon.manager.HotbarManager.spoofHotbarBypass
import dev.zenhao.melon.manager.RotationManager
import dev.zenhao.melon.module.Category
import dev.zenhao.melon.module.Module
import dev.zenhao.melon.module.modules.player.AntiMinePlace
import dev.zenhao.melon.module.modules.player.PacketMine.hookPos
import dev.zenhao.melon.utils.TimerUtils
import dev.zenhao.melon.utils.animations.sq
import dev.zenhao.melon.utils.inventory.HotbarSlot
import melon.system.event.SafeClientEvent
import melon.utils.block.BlockUtil.getNeighbor
import melon.utils.chat.ChatUtil
import melon.utils.combat.getTarget
import melon.utils.concurrent.threads.runSafe
import melon.utils.entity.EntityUtils.boxCheck
import melon.utils.entity.EntityUtils.isInWeb
import melon.utils.entity.EntityUtils.spoofSneak
import melon.utils.extension.fastPos
import melon.utils.hole.SurroundUtils
import melon.utils.hole.SurroundUtils.checkHole
import melon.utils.inventory.slot.allSlots
import melon.utils.inventory.slot.firstItem
import melon.utils.inventory.slot.hotbarSlots
import melon.utils.world.noCollision
import net.minecraft.block.Blocks
import net.minecraft.block.PistonBlock
import net.minecraft.block.RedstoneBlock
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import team.exception.melon.util.math.distanceSqToCenter

object HolePush : Module(
    name = "HolePush",
    langName = "活塞推人",
    category = Category.COMBAT,
    description = "Push the target away from the hole."
) {
    private val targetRange = dsetting("TargetRange", 7.0, 0.0, 10.0)
    private val range by isetting("Range", 5, 0, 6)
    private val swing = bsetting("Swing", true)
    private val packet by bsetting("packet", true).isTrue(swing)
    private val rotate = bsetting("Rotation", false)
    private val side by bsetting("Side", false).isTrue(rotate)
    private val spoofBypass = bsetting("SpoofBypass", false)
    private val invSwap by bsetting("InvSwap", false)
    private val strictDirection = bsetting("StrictDirection", false)
    private val checkDown by bsetting("CheckDown", false)
    private val delay by dsetting("Delay", 50.0, 0.0, 250.0)
    private val airPlace by bsetting("AirPlace", false)
    private val autoToggle = bsetting("AutoToggle", true)
    private val pushDelay by dsetting("PushDelay", 250.0, 0.0, 1000.0).isFalse(autoToggle)
    private val debug by bsetting("Debug", false)
    private val timer = TimerUtils()
    private val pushTimer = TimerUtils()
    private var stage = 0

    override fun onEnable() {
        runSafe {
            stage = 0
        }
    }

    override fun onDisable() {
        runSafe { stage = 0 }
    }

    init {
        onMotion {
            var pistonSlot =
                player.hotbarSlots.firstItem(Items.STICKY_PISTON) ?: player.hotbarSlots.firstItem(Items.PISTON)
            var stoneSlot = player.hotbarSlots.firstItem(
                Items.REDSTONE_BLOCK
            )
            if (spoofBypass.value && invSwap) {
                pistonSlot = player.allSlots.firstItem(Items.STICKY_PISTON)
                    ?.let { item -> HotbarSlot(item) } ?: player.allSlots.firstItem(Items.PISTON)
                    ?.let { item -> HotbarSlot(item) } ?: pistonSlot
                stoneSlot = player.allSlots.firstItem(Items.REDSTONE_BLOCK)
                    ?.let { item -> HotbarSlot(item) } ?: stoneSlot
            }
            val target = getTarget(targetRange.value)

            if (pistonSlot == null || stoneSlot == null || target == null) {
                if (autoToggle.value) {
                    toggle()
                }
                return@onMotion
            }
            if (autoToggle.value && stage >= 4) {
                toggle()
                return@onMotion
            }
            if (!autoToggle.value) {
                if ((world.isAir(target.blockPos) && checkHole(target) == SurroundUtils.HoleType.NONE) || player.usingItem)
                    return@onMotion
            }
            if (!world.isAir(target.blockPos.up(2))) return@onMotion
            val targetUp = target.blockPos.up()
            if (pushTimer.passedMs(pushDelay.toLong())) {
                if (!world.isAir(targetUp.up())) return@onMotion
                if (isInWeb(target)) return@onMotion
                doHolePush(targetUp, true, pistonSlot, stoneSlot)
                if (!world.isAir(target.blockPos)) doHolePush(targetUp, false, pistonSlot, stoneSlot)
            }
        }
    }

    fun SafeClientEvent.doHolePush(
        targetPos: BlockPos,
        check: Boolean,
        pistonSlot: HotbarSlot?,
        stoneSlot: HotbarSlot?
    ): BlockPos? {
        fun checkPull(face: Direction): Boolean {
            val opposite = targetPos.offset(face.opposite)
            return when (face) {
                Direction.NORTH -> {
                    !world.isAir(opposite) || !world.isAir(opposite.up())
                }

                Direction.SOUTH -> {
                    !world.isAir(opposite) || !world.isAir(opposite.up())
                }

                Direction.EAST -> {
                    !world.isAir(opposite) || !world.isAir(opposite.up())
                }

                Direction.WEST -> {
                    !world.isAir(opposite) || !world.isAir(opposite.up())
                }

                else -> false
            }
        }
        for (face in Direction.entries) {
            if (face == Direction.DOWN || face == Direction.UP) continue
            if (checkPull(face) && check) continue
            if (checkPull(face) && (!world.isAir(targetPos.offset(face).up()) && world.getBlockState(
                    targetPos.offset(
                        face
                    ).up()
                ).block !is RedstoneBlock)
            ) continue
            if (!world.entities.none {
                    it !is ItemEntity;it.isAlive;it.boundingBox.intersects(
                    Box(
                        targetPos.offset(
                            face
                        )
                    )
                )
                }) continue
//            if (!world.noCollision(targetPos.offset(face)) && world.getBlockState(targetPos.offset(face)).block !is PistonBlock) continue
            if (!world.isAir(targetPos.offset(face)) && world.getBlockState(targetPos.offset(face)).block !is PistonBlock) continue
            getRedStonePos(targetPos.offset(face), face)?.let {
                if (!world.isAir(it.pos.down()) || !checkDown) {
                    if (pistonSlot != null && stoneSlot != null) {
                        placeBlock(
                            targetPos.offset(face),
                            it.pos,
                            face,
                            pistonSlot,
                            stoneSlot,
                            !check
                        )
                    }
                    return it.pos
                } else if (!world.isAir(it.pos.down(2)) && checkDown && it.direction == Direction.DOWN) {
                    if (timer.tickAndReset(delay)) {
                        if (rotate.value) RotationManager.addRotations(it.pos.down())
                        player.spoofSneak {
                            var slot =
                                player.hotbarSlots.firstItem(Items.OBSIDIAN)
                            if (spoofBypass.value && invSwap) {
                                slot = player.allSlots.firstItem(Items.OBSIDIAN)
                                    ?.let { item -> HotbarSlot(item) } ?: stoneSlot
                            }
                            slot?.let { obs ->
                                if (spoofBypass.value) {
                                    spoofHotbarBypass(obs) {
                                        connection.sendPacket(fastPos(it.pos.down(), strictDirection.value))
                                    }
                                } else {
                                    spoofHotbar(obs) {
                                        connection.sendPacket(fastPos(it.pos.down(), strictDirection.value))
                                    }
                                }
                            }
                        }
                    }
                }
            } ?: continue
        }
        return null
    }

    private fun SafeClientEvent.placeBlock(
        blockPos: BlockPos,
        stonePos: BlockPos,
        face: Direction,
        pistonSlot: HotbarSlot,
        stoneSlot: HotbarSlot,
        mine: Boolean = false
    ) {
        if (!timer.passedMs(delay.toLong())) return
        fun spoofPlace(stone: Boolean, doToggle: Boolean = false) {
            if (!stone) {
                RotationManager.addRotations(blockPos = blockPos, prio = true, side = true)
                RotationManager.stopRotation()
                face.let {
                    connection.sendPacket(
                        PlayerMoveC2SPacket.LookAndOnGround(
                            when (it) {
                                Direction.EAST -> -90f
                                Direction.NORTH -> 180f
                                Direction.SOUTH -> 0f
                                Direction.WEST -> 90f
                                else -> 0f
                            }, player.pitch, true
                        )
                    )
                }
            }
            RotationManager.startRotation()
            if (!stone) RotationManager.addRotations(blockPos, true)
            if (!stone || world.isAir(stonePos)) {
                player.spoofSneak {
                    if (spoofBypass.value) {
                        spoofHotbarBypass(if (!stone) pistonSlot else stoneSlot) {
                            connection.sendPacket(fastPos(if (!stone) blockPos else stonePos, strictDirection.value))
                        }
                    } else {
                        spoofHotbar(if (!stone) pistonSlot else stoneSlot) {
                            connection.sendPacket(fastPos(if (!stone) blockPos else stonePos, strictDirection.value))
                        }
                    }
                }
                swingHand()
            }
            if (!stone) RotationManager.addRotations(blockPos, true)
            stage++
            if (debug) ChatUtil.sendMessage(if (stone) "[HolePush] -> PlaceStone!" else "[HolePush] -> PlacePiston!")
            if (!world.isAir(stonePos) && doToggle) {
                if (debug) ChatUtil.sendMessage("[HolePush] -> Doing Toggle!")
                if (mine) hookPos(stonePos)
                if (autoToggle.value) {
                    toggle()
                } else {
                    pushTimer.reset()
                    stage = 0
                }
            }
            timer.reset()
            return
        }
        if (getNeighbor(blockPos, false) != null || airPlace) {
            if (world.isAir(blockPos)) {
                if (rotate.value) {
                    RotationManager.addRotations(blockPos, side = side)
                }
                spoofPlace(stone = false, doToggle = true)
            } else {
                if (rotate.value && world.isAir(stonePos)) {
                    RotationManager.addRotations(stonePos, side = side)
                }
                spoofPlace(stone = true, doToggle = true)
            }
        } else {
            if (rotate.value && world.isAir(stonePos)) {
                RotationManager.addRotations(stonePos, side = side)
            }
            spoofPlace(stone = true, doToggle = false)
        }
    }

    private fun SafeClientEvent.swingHand() {
        if (!swing.value) return
        if (packet) {
            connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
        } else player.swingHand(Hand.MAIN_HAND)
    }

    fun SafeClientEvent.getRedStonePos(pos: BlockPos, direction: Direction): StonePos? {
        val face = when (direction) {
            Direction.EAST -> Direction.WEST
            Direction.WEST -> Direction.EAST
            Direction.NORTH -> Direction.SOUTH
            Direction.SOUTH -> Direction.NORTH
            else -> direction
        }
        for (facing: Direction in Direction.entries) {
            if (world.getBlockState(pos.offset(facing)).block == Blocks.REDSTONE_BLOCK) {
                return StonePos(pos.offset(facing), facing)
            }
            if (facing != face) {
                if (player.distanceSqToCenter(pos.offset(facing)) > range.sq) continue
                if (!boxCheck(Box(pos.offset(facing)))) continue
                if (!world.noCollision(pos.offset(facing))) continue
                if (!world.isAir(pos.offset(facing))) continue
                if (getNeighbor(pos.offset(facing), false) == null) continue
                val minePos = AntiMinePlace.mineMap[pos.offset(facing)]
                if (AntiMinePlace.isEnabled && minePos != null) {
                    if (System.currentTimeMillis() - minePos.mine >= minePos.start
                    ) continue
                }
                return StonePos(pos.offset(facing), facing)
            }
        }
        return null
    }

    class StonePos(var pos: BlockPos, var direction: Direction)
}