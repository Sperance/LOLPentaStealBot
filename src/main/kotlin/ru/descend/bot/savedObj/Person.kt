package ru.descend.bot.savedObj

import ru.descend.bot.firebase.FirePerson

data class DataBasic(
    var user: FirePerson? = null,
    var text: String = "",
    var date: Long = System.currentTimeMillis()
)