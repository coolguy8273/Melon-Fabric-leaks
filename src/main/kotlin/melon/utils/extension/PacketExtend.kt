package melon.utils.extension

import com.google.common.collect.Lists
import dev.zenhao.melon.manager.CrystalManager
import dev.zenhao.melon.module.modules.player.PacketMine.BlockData
import dev.zenhao.melon.module.modules.player.PacketMine.PacketType
import dev.zenhao.melon.module.modules.render.PlaceRender
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import melon.system.event.SafeClientEvent
import melon.utils.block.BlockUtil
import melon.utils.block.BlockUtil.getNeighbor
import melon.utils.concurrent.threads.runSynchronized
import melon.utils.entity.EntityUtils.eyePosition
import melon.utils.player.RotationUtils
import melon.utils.player.updateController
import melon.utils.world.getMiningSide
import net.minecraft.client.network.SequencedPacketCreator
import net.minecraft.client.world.ClientWorld
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import team.exception.melon.util.math.toBlockPos
import team.exception.melon.util.math.toVec3d

fun fastPosDirection(
    pos: BlockPos,
    facing: Direction = Direction.UP,
    hand: Hand = Hand.MAIN_HAND,
    inside: Boolean = false,
    sequence: Int = 0
): PlayerInteractBlockC2SPacket {
    return PlayerInteractBlockC2SPacket(
        hand,
        BlockHitResult(pos.toCenterPos(), facing, pos, inside),
        sequence
    )
}

fun fastPosDirectionDown(
    pos: BlockPos,
    hand: Hand = Hand.MAIN_HAND,
    inside: Boolean = false,
    sequence: Int = 0
): PlayerInteractBlockC2SPacket {
    return PlayerInteractBlockC2SPacket(
        hand,
        BlockHitResult(pos.toCenterPos(), Direction.DOWN, pos, inside),
        sequence
    )
}

fun SafeClientEvent.fastPos(
    pos: BlockPos,
    strictDirection: Boolean = false,
    face: Direction = Direction.UP,
    hand: Hand = Hand.MAIN_HAND,
    inside: Boolean = false,
    sequence: Int = 0,
    render: Boolean = true
): PlayerInteractBlockC2SPacket {
    val placePos = getNeighbor(pos, strictDirection) ?: BlockUtil.EasyBlock(pos, face)
    if (render) PlaceRender.renderBlocks[pos] = System.currentTimeMillis()
    return PlayerInteractBlockC2SPacket(
        hand,
        BlockHitResult(
            pos.toCenterPos(),
            placePos.face,
            placePos.blockPos,
            inside
        ),
        sequence
    )
}

fun SafeClientEvent.fastPos(
    vec: Vec3d,
    strictDirection: Boolean = false,
    face: Direction = Direction.UP,
    hand: Hand = Hand.MAIN_HAND,
    inside: Boolean = false,
    sequence: Int = 0
): PlayerInteractBlockC2SPacket {
    val placePos = getNeighbor(vec.toBlockPos(), strictDirection) ?: BlockUtil.EasyBlock(vec.toBlockPos(), face)
    PlaceRender.renderBlocks[vec.toBlockPos()] = System.currentTimeMillis()
    return PlayerInteractBlockC2SPacket(
        hand,
        BlockHitResult(
            vec,
            placePos.face,
            placePos.blockPos,
            inside
        ),
        sequence
    )
}

fun SafeClientEvent.position(yOffset: Int = 0, onGround: Boolean = false): PlayerMoveC2SPacket.PositionAndOnGround {
    return PlayerMoveC2SPacket.PositionAndOnGround(
        player.x,
        player.y + yOffset,
        player.z,
        onGround
    )
}

fun SafeClientEvent.position(yOffset: Double = 0.0, onGround: Boolean = false): PlayerMoveC2SPacket.PositionAndOnGround {
    return PlayerMoveC2SPacket.PositionAndOnGround(
        player.x,
        player.y + yOffset,
        player.z,
        onGround
    )
}

fun SafeClientEvent.positionRotation(yOffset: Double = 0.0, onGround: Boolean = false): PlayerMoveC2SPacket.Full {
    return PlayerMoveC2SPacket.Full(
        player.x,
        player.y + yOffset,
        player.z,
        CrystalManager.rotation.x,
        90f,
        onGround
    )
}

fun SafeClientEvent.positionRotation(yOffset: Double = 0.0, blockPos: BlockPos, onGround: Boolean = false): PlayerMoveC2SPacket.Full {
    return PlayerMoveC2SPacket.Full(
        player.x,
        player.y + yOffset,
        player.z,
        RotationUtils.getLegitRotations(blockPos.toVec3d())[0],
        RotationUtils.getLegitRotations(blockPos.toVec3d())[1],
        onGround
    )
}

fun positionBypass(
    vec: BlockPos,
    ground: Boolean = false
): PlayerMoveC2SPacket.PositionAndOnGround {
    return PlayerMoveC2SPacket.PositionAndOnGround(
        vec.x.toDouble(),
        vec.y.toDouble(),
        vec.z.toDouble(),
        ground
    )
}

fun positionRotationBypass(
    vec: BlockPos,
    ground: Boolean = false
): PlayerMoveC2SPacket.Full {
    return PlayerMoveC2SPacket.Full(
        vec.x.toDouble(),
        vec.y.toDouble(),
        vec.z.toDouble(),
        CrystalManager.rotation.x,
        90f,
        ground
    )
}

fun SafeClientEvent.minePacket(actionType: PacketType, blockData: BlockData, sequence: Int): PlayerActionC2SPacket {
    val side = getMiningSide(blockData.blockPos) ?: run {
        val vector = player.eyePosition.subtract(blockData.blockPos.x + 0.5, blockData.blockPos.y + 0.5, blockData.blockPos.z + 0.5)
        Direction.getFacing(vector.x.toFloat(), vector.y.toFloat(), vector.z.toFloat())
    }
    return PlayerActionC2SPacket(
        actionType.action,
        blockData.blockPos,
        side,
        sequence
    )
}

fun SafeClientEvent.packetClick(slot: Int, clickType: SlotActionType = SlotActionType.PICKUP): ClickSlotC2SPacket {
    val defaultedList = player.currentScreenHandler.slots
    val i = defaultedList.size
    val list = Lists.newArrayListWithCapacity<ItemStack>(i)
    for (slot0 in defaultedList) {
        list.add(slot0.stack.copy())
    }
    val int2ObjectMap = Int2ObjectOpenHashMap<ItemStack>()
    for (j in 0 until i) {
        var itemStack2: ItemStack
        if (ItemStack.areEqual(list[j], defaultedList[j].stack.also { itemStack2 = it })) continue
        int2ObjectMap.runSynchronized {
            this[j] = itemStack2.copy()
        }
    }
    return ClickSlotC2SPacket(
        player.playerScreenHandler.syncId,
        player.currentScreenHandler.nextRevision(),
        slot,
        0,
        clickType,
        player.inventory.getStack(slot),
        int2ObjectMap
    )
}

fun SafeClientEvent.sendSequencedPacket(world: ClientWorld, packetCreator: SequencedPacketCreator) {
    world.pendingUpdateManager.incrementSequence().use { pendingUpdateManager ->
        val i = pendingUpdateManager.sequence
        val packet = packetCreator.predict(i)
        playerController.updateController()
        connection.sendPacket(packet)
    }
}