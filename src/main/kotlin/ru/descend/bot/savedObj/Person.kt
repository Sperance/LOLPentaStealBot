package ru.descend.bot.savedObj

import ru.descend.bot.postgre.TableKORD_LOL
import ru.descend.bot.postgre.TableMatch

data class DataBasic(
    var user: TableKORD_LOL? = null,
    var text: String = "",
    var date: Long = System.currentTimeMillis(),
    var match: TableMatch
)