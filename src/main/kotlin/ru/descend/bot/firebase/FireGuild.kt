package ru.descend.bot.firebase

import com.google.firebase.database.Exclude
import dev.kord.core.entity.Guild

data class FireGuild (
    var id: String = "",
    var name: String = "",
    var applicationId: String? = null,
    var description: String = "",
    var ownerId: String = "",

    var botChannelId: String = "",
    var messageId: String = ""
) : FireBaseData() {
    @Exclude
    fun initGuild(guild: Guild) {
        this.id = guild.id.value.toString()
        this.name = guild.name
        this.applicationId = guild.applicationId?.value.toString()
        this.description = guild.description ?: ""
        this.ownerId = guild.ownerId.value.toString()
    }
}