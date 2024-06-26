package dev.zenhao.melon.manager

import melon.utils.Wrapper
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity

object FriendManager {
    var friends = ArrayList<Friend>()

    fun checkExist(name: String): Boolean {
        for (friend in friends) {
            if (!friend.name.equals(name, true)) continue
            return true
        }
        return false
    }

    fun getFriendByName(name: String): Friend? {
        for (friend in friends) {
            if (!friend.name.equals(name, true)) continue
            return friend
        }
        return null
    }

    fun addFriend(name: String) {
        if (!checkExist(name)) {
            friends.add(Friend(name, true))
        } else {
            getFriendByName(name)?.isFriend = true
        }
    }

    fun removeFriend(name: String) {
        if (checkExist(name)) {
            getFriendByName(name)?.isFriend = false
        }
    }

    fun isFriend(name: String): Boolean {
        Wrapper.player?.let {
            if (name == it.entityName) return true
        }
        return if (!checkExist(name)) {
            false
        } else getFriendByName(name)?.isFriend ?: false
    }

    fun isFriend(entity: Entity): Boolean {
        return isFriend(entity.entityName)
    }

    val friendStringList: List<String>
        get() {
            val stringList = ArrayList<String>()
            friends.forEach { stringList.add(it.name) }
            return stringList
        }


    class Friend(var name: String, var isFriend: Boolean)
}
