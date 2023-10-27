package ru.descend.bot.data

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.cache.data.UserData
import dev.kord.core.entity.User
import kotlinx.serialization.Serializable
import me.jakejmattson.discordkt.dsl.Data
import java.util.Properties

object Configuration {
    val botOwnerId: Snowflake = Snowflake(948280038137139200)
    val botCurrentId: Snowflake = Snowflake(1167141517631168542)

    val prefix: String = "ll."
    val botName: String = "LOLPentaSteal"

    fun getBotAsUser(kord: Kord): User {
        return User(data = UserData(botCurrentId, botName, ""), kord = kord)
    }
}