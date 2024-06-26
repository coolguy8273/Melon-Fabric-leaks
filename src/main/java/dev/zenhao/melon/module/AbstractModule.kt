package dev.zenhao.melon.module

import dev.zenhao.melon.module.hud.NotificationHUD
import dev.zenhao.melon.module.modules.client.ClickGui
import dev.zenhao.melon.setting.*
import melon.events.ModuleEvent
import melon.events.PacketEvents
import melon.events.RunGameLoopEvent
import melon.events.TickEvent
import melon.events.input.BindEvent
import melon.events.player.PlayerMotionEvent
import melon.events.render.Render2DEvent
import melon.events.render.Render3DEvent
import melon.notification.NotificationManager
import melon.system.event.ListenerOwner
import melon.system.event.SafeClientEvent
import melon.system.event.safeConcurrentListener
import melon.system.event.safeEventListener
import melon.system.util.IDRegistry
import melon.system.util.color.ColorRGB
import melon.utils.Wrapper
import melon.utils.chat.ChatUtil
import net.minecraft.client.gui.DrawContext
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.util.*

abstract class AbstractModule : ListenerOwner() {
    val settingList = ArrayList<Setting<*>>()
    var moduleName = ""
    var moduleCName = ""
    var moduleCategory = Category.HIDDEN
    var alwaysEnable = false
    var isEnabled = false
    var isVisible = false
        set(value) {
            field = value
            ModuleEvent.VisibleChange(this).post()
        }
    var description: String = ""

    private val idRegistry = IDRegistry()
    val id = idRegistry.register()
    var holdToEnable = false
    var bind: Int = 0

    init {
        safeConcurrentListener<TickEvent.Pre> {
            if (alwaysEnable) {
                isEnabled = true
            }
        }
    }

    fun enable() {
        if (alwaysEnable) {
            return
        }
        isEnabled = true
        ModuleEvent.Toggle(this).post()
        onEnable()
        subscribe()
    }

    fun disable() {
        if (alwaysEnable) {
            return
        }
        isEnabled = false
        ModuleEvent.Toggle(this).post()
        onDisable()
        unsubscribe()
    }

    fun safeDisable() {
        if (isEnabled) {
            disable()
        }
    }

    inline fun onRender3D(crossinline safeEvent: SafeClientEvent.(Render3DEvent) -> Unit) {
        safeEventListener<Render3DEvent> { event ->
            if (isEnabled) {
                safeEvent.invoke(this@safeEventListener, event)
            }
        }
    }

    inline fun onRender2D(crossinline safeEvent: SafeClientEvent.(Render2DEvent) -> Unit) {
        safeEventListener<Render2DEvent> { event ->
            if (isEnabled) {
                safeEvent.invoke(this@safeEventListener, event)
            }
        }
    }

    inline fun onPacketSend(crossinline safeEvent: SafeClientEvent.(PacketEvents.Send) -> Unit) {
        safeEventListener<PacketEvents.Send> { event ->
            if (isEnabled) {
                safeEvent.invoke(this@safeEventListener, event)
            }
        }
    }

    inline fun onPacketReceive(crossinline safeEvent: SafeClientEvent.(PacketEvents.Receive) -> Unit) {
        safeEventListener<PacketEvents.Receive> { event ->
            if (isEnabled) {
                safeEvent.invoke(this@safeEventListener, event)
            }
        }
    }

    inline fun onMotion(crossinline safeEvent: SafeClientEvent.(PlayerMotionEvent) -> Unit) {
        safeEventListener<PlayerMotionEvent> { event ->
            if (isEnabled) {
                safeEvent.invoke(this@safeEventListener, event)
            }
        }
    }

    inline fun onLoop(crossinline safeEvent: SafeClientEvent.(RunGameLoopEvent.Tick) -> Unit) {
        safeEventListener<RunGameLoopEvent.Tick> { event ->
            if (isEnabled) {
                safeEvent.invoke(this@safeEventListener, event)
            }
        }
    }

    fun bsetting(name: String, defaultValue: Boolean): BooleanSetting {
        val value = BooleanSetting(name, this, defaultValue)
        settingList.add(value)
        return value
    }

    fun isetting(name: String, defaultValue: Int, minValue: Int, maxValue: Int, modifyValue: Int = 0): IntegerSetting {
        val value = IntegerSetting(name, this, defaultValue, minValue, maxValue, modifyValue)
        settingList.add(value)
        return value
    }

    fun fsetting(
        name: String, defaultValue: Float, minValue: Float, maxValue: Float, modifyValue: Float = 0f
    ): FloatSetting {
        val value = FloatSetting(name, this, defaultValue, minValue, maxValue, modifyValue)
        settingList.add(value)
        return value
    }

    fun dsetting(
        name: String, defaultValue: Double, minValue: Double, maxValue: Double, modifyValue: Double = 0.0
    ): DoubleSetting {
        val value = DoubleSetting(name, this, defaultValue, minValue, maxValue, modifyValue)
        settingList.add(value)
        return value
    }

    fun isetting(name: String, defaultValue: Int, minValue: Int, maxValue: Int): IntegerSetting {
        val value = IntegerSetting(name, this, defaultValue, minValue, maxValue, 0)
        settingList.add(value)
        return value
    }

    fun fsetting(name: String, defaultValue: Float, minValue: Float, maxValue: Float): FloatSetting {
        val value = FloatSetting(name, this, defaultValue, minValue, maxValue, 0f)
        settingList.add(value)
        return value
    }

    fun dsetting(name: String, defaultValue: Double, minValue: Double, maxValue: Double): DoubleSetting {
        val value = DoubleSetting(name, this, defaultValue, minValue, maxValue, 0.0)
        settingList.add(value)
        return value
    }

    fun msetting(name: String, modes: Enum<*>): ModeSetting<*> {
        val value: ModeSetting<*> = ModeSetting(name, this, modes)
        settingList.add(value)
        return value
    }

    fun ssetting(name: String, defaultValue: String): StringSetting {
        val value = StringSetting(name, this, defaultValue)
        settingList.add(value)
        return value
    }

    fun csetting(name: String, defaultValue: Color): ColorSetting {
        val value = ColorSetting(name, this, defaultValue)
        settingList.add(value)
        return value
    }

    fun csetting(name: String, defaultValue: ColorRGB): ColorSetting {
        val value = ColorSetting(name, this, Color(defaultValue.r, defaultValue.g, defaultValue.b))
        settingList.add(value)
        return value
    }

    fun getBindName(): String {
        var kn = if (bind in 3..4) {
            when (bind) {
                3 -> "Mouse_4"
                4 -> "Mouse_5"
                else -> "None"
            }
        } else {
            if (this.bind > 0) GLFW.glfwGetKeyName(bind, GLFW.glfwGetKeyScancode(bind)) else "None"
        }
        if (kn == null) {
            try {
                for (declaredField in GLFW::class.java.declaredFields) {
                    if (declaredField.name.startsWith("GLFW_KEY_")) {
                        val a = declaredField[null] as Int
                        if (a == bind) {
                            val nb = declaredField.name.substring("GLFW_KEY_".length)
                            kn = nb.substring(0, 1).uppercase(Locale.getDefault()) + nb.substring(1)
                                .lowercase(Locale.getDefault())
                        }
                    }
                }
            } catch (ignored: Exception) {
                kn = "unknown.$bind"
            }
        }
        return if (bind == -1) "None" else (kn + "").uppercase(Locale.getDefault())
    }

    val isDisabled: Boolean
        get() = !isEnabled

    open fun getHudInfo(): String? = null
    open fun onConfigLoad() {}
    open fun onConfigSave() {}
    open fun onEnable() {}
    open fun onDisable() {}
    open fun onLogout() {}
    open fun onRender(context: DrawContext) {}

    open fun getCName(): String {
        return moduleCName
    }

    open fun getName(): String {
        return if (ClickGui.chinese.value) moduleCName else moduleName
    }

    fun toggle() {
        isEnabled = !isEnabled
        if (isEnabled) {
            enable()
            if (ClickGui.chat.value) ChatUtil.sendMessageWithID(moduleName + " is " + ChatUtil.GREEN + "Enabled!", 11451)
            if (NotificationHUD.isEnabled && ClickGui.notification) NotificationManager.addNotification((if (ClickGui.chinese.value) moduleCName else moduleName) + ChatUtil.GREEN + " is Enable!")
        } else {
            disable()
            if (ClickGui.chat.value) ChatUtil.sendMessageWithID(moduleName + " is " + ChatUtil.RED + "Disabled!", 11451)
            if (NotificationHUD.isEnabled && ClickGui.notification) NotificationManager.addNotification((if (ClickGui.chinese.value) moduleCName else moduleName) + ChatUtil.RED + " is Disable!")
        }
    }

    @Suppress("unused")
    fun onKey(event: BindEvent) {
    }

    fun setEnable(toggled: Boolean) {
        isEnabled = toggled
    }

    fun isHidden(): Boolean {
        return moduleCategory.isHidden
    }

    fun getArrayList(): String {
        return if (ClickGui.chinese.value) moduleCName else moduleName + if (getHudInfo() == null || getHudInfo() == "") "" else " " + ChatUtil.SECTIONSIGN + "7" + (if (getHudInfo() == "" || getHudInfo() == null) "" else "[") + ChatUtil.SECTIONSIGN + "f" + getHudInfo() + '\u00a7' + "7" + if (getHudInfo() == "") "" else "]"
    }

    companion object {
        @JvmField
        val mc = Wrapper.minecraft
    }
}