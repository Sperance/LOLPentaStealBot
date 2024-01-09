package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import dev.kord.core.entity.Guild
import ru.descend.bot.firebase.FireMatch
import ru.descend.bot.firebase.FirePerson
import save
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
    val matches: List<FireMatchTable> by oneToMany(FireMatchTable::guild)

    fun addPersonFire(it: FirePerson) {
        FirePersonTable(
            personIndex = it.personIndex,
            KORD_id = it.KORD_id,
            KORD_discriminator = it.KORD_discriminator,
            KORD_name = it.KORD_name,
            LOL_accountId = it.LOL_accountId,
            LOL_id = it.LOL_id,
            LOL_name = it.LOL_name,
            LOL_profileIconId = it.LOL_profileIconId,
            LOL_puuid = it.LOL_puuid,
            LOL_region = it.LOL_region,
            guild = this
        ).save()
    }

    fun addMatchFire(it: FireMatch) {

        if (matches.find { mat -> mat.matchId == it.matchId } != null) return

        val pMatch = FireMatchTable(
            matchId = it.matchId,
            matchDate = it.matchDate,
            matchDuration = it.matchDuration,
            matchMode = it.matchMode,
            matchGameVersion = it.matchGameVersion,
            guild = this
        )
        pMatch.save()
        pMatch.addParticipants(it)
    }

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