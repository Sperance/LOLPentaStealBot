package ru.descend.derpg.enums

import ru.descend.derpg.model.IntStat

enum class EnumStatBool(val code: String): IntStat {
    IS_ALIVE("BL0"),
    IS_BANNED("BL1")
}