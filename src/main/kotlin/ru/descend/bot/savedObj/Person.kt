package ru.descend.bot.savedObj

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.User
import kotlinx.serialization.Serializable

@Serializable
class Person {

    var uid: ULong
    var name: String

    var pentaKills: ULong = 0u
    var pentaStills: ArrayList<Pair<ULong, ULong>> = ArrayList()

    constructor(uid: ULong, name: String) {
        this.uid = uid
        this.name = name
    }

    companion object {
        fun create(user: User): Person {
            return Person(uid = user.data.id.value, name = user.username)
        }
    }

    fun getSnowFlake() = Snowflake(uid)
}