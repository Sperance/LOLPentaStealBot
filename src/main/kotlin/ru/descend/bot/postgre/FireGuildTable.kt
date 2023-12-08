package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import dev.kord.core.entity.Guild
import table

data class FireGuildTable (
    override var id: Int = 0,
    var idGuild: String = "",
    var name: String = "",
    var applicationId: String? = null,
    var description: String = "",
    var ownerId: String = "",

    var botChannelId: String = "",
    var messageId: String = "",
    var messageIdPentaData: String = "",
    var messageIdGlobalStatisticData: String = "",
    var messageIdMasteryData: String = "",
): Entity() {

    val persons: List<FirePersonTable> by oneToMany(FirePersonTable::guild)

    fun initGuild(guild: Guild) {
        this.idGuild = guild.id.value.toString()
        this.name = guild.name
        this.applicationId = guild.applicationId?.value.toString()
        this.description = guild.description ?: ""
        this.ownerId = guild.ownerId.value.toString()
    }
}

val fireGuildTable = table<FireGuildTable, Database> {
    column(FireGuildTable::idGuild).unique()
}