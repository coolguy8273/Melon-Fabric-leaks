package dev.zenhao.melon.command.argument.impl

import dev.zenhao.melon.command.argument.Argument
import melon.system.util.interfaces.Alias

class StringArgument(
    index: Int,
    override val name: String,
    override val alias: Array<out String>,
    private val ignoreCase: Boolean
) : Argument<String>(index), Alias {
    override fun complete(input: String): List<String>? {
        return if (name.startsWith(input)) {
            listOf(name)
        } else {
            alias.filter {
                it.startsWith(input, ignoreCase)
            }
        }
    }

    override fun convertToType(input: String): String? {
        return if (input.equals(name, ignoreCase) || alias.contains(input)) {
            input
        } else {
            null
        }
    }

    override fun toString(): String {
        return name
    }
}