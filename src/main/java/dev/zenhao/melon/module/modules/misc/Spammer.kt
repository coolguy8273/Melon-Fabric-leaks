package dev.zenhao.melon.module.modules.misc

import dev.zenhao.melon.module.Category
import dev.zenhao.melon.module.Module
import dev.zenhao.melon.utils.TimerUtils
import net.minecraft.text.Text
import kotlin.random.Random

object Spammer : Module(name = "Spammer", langName = "刷屏", category = Category.MISC) {
    private val text by ssetting("Text", "")
    private val delay by isetting("Delay(S)", 60, 1, 600)
    private val antiKick by bsetting("Anti Kick", true)

    private val timer = TimerUtils()

    init {
        onLoop {
            if (timer.tickAndReset(delay * 1000)) {
                mc.player?.networkHandler?.sendChatMessage(if (antiKick) "$text - ${generateRandomLetters(4)}" else text)
            }
        }
    }

    private fun generateRandomLetters(length: Int): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890" // 可以根据需要修改字母集合
        return (1..length)
            .map { alphabet[Random.nextInt(alphabet.length)] }
            .joinToString("")
    }
}