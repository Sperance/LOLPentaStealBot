package ru.descend.bot

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.TextChannel
import dev.kord.gateway.ALL
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.embed
import dev.kord.rest.builder.message.modify.UserMessageModifyBuilder
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.delay
import me.jakejmattson.discordkt.dsl.bot
import me.jakejmattson.discordkt.util.TimeStamp
import ru.descend.bot.datas.LolActiveGame
import ru.descend.bot.enums.EnumMMRRank
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.dto.currentGameInfo.Participant
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.Participants.Companion.tbl_participants
import ru.descend.bot.postgre.r2dbc.update
import ru.descend.bot.savedObj.isCurrentDay
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(PrivilegedIntent::class)
@KordPreview
fun main() {
    bot(catchToken()[0]) {
        prefix {
            "ll."
        }
        configure {
            logStartup = true
            documentCommands = true
            recommendCommands = true
            searchCommands = true
            deleteInvocation = true
            dualRegistry = true
            commandReaction = Emojis.adult
            theme = Color(0x00B92F)
            intents = Intents.ALL
            defaultPermissions = Permissions(Permission.UseApplicationCommands)
        }
        onException {
            println("Exception '${exception::class.simpleName}': ${exception.message}")
        }
        presence {
            this.status = PresenceStatus.Online
            playing("ARAM")
        }
        kord {
            enableShutdownHook = true
            stackTraceRecovery = true
        }
        onStart {
            firstInitialize()
            kord.guilds.collect {
                val guilds = R2DBC.getGuild(it)
                mapMainData[it] = SQLData_R2DBC(it, guilds)
                mapMainData[it]?.initialize()

                removeMessage(it)

                timerRequestReset((2).minutes)
                timerMainInformation(it, (5).minutes)
//                timerRealtimeInformation(it, (5).minutes, skipFirst = true)
            }
        }
    }
}

val mapMainData = HashMap<Guild, SQLData_R2DBC>()

fun timerRequestReset(duration: Duration) = launch {
    while (true) {
        globalLOLRequests = 0
        delay(duration)
    }
}

fun timerMainInformation(guild: Guild, duration: Duration, skipFirst: Boolean = false) = launch {
    var skipped = skipFirst
    while (true) {
        if (mapMainData[guild]!!.guildSQL.botChannelId.isNotEmpty()) {
            if (!skipped) {
                showLeagueHistory(mapMainData[guild]!!)
                printMemoryUsage()
            } else {
                skipped = true
            }
        }
        delay(duration)
    }
}

private suspend fun firstInitialize() {
    LeagueMainObject.catchHeroNames()
    R2DBC.initialize()
}

suspend fun removeMessage(guild: Guild) {
    if (mapMainData[guild]!!.guildSQL.botChannelId.isNotEmpty()){
        guild.getChannelOf<TextChannel>(Snowflake(mapMainData[guild]!!.guildSQL.botChannelId)).messages.collect {
            if (it.id.value.toString() in listOf(mapMainData[guild]!!.guildSQL.messageIdGlobalStatisticData, mapMainData[guild]!!.guildSQL.messageIdMain, mapMainData[guild]!!.guildSQL.messageIdArammmr, mapMainData[guild]!!.guildSQL.messageIdMasteries)) {
                Unit
            } else {
                it.delete()
            }
        }
    }
}

var globalLOLRequests = 0
var statusLOLRequests = 0

suspend fun showRealtimeHistory(sqlData: SQLData_R2DBC) {
    val channel = sqlData.guild.getChannelOf<TextChannel>(Snowflake(1225762638735085620))
    val arrayActives = ArrayList<LolActiveGame>()
    sqlData.dataSavedLOL.get().forEach {
        if (it.LOL_puuid.isEmpty()) return@forEach
        if (arrayActives.find { ara -> ara.part.puuid == it.LOL_puuid } != null) return@forEach
        val kordlol = sqlData.getKORDLOL().firstOrNull { kd -> kd.LOL_id == it.id && kd.guild_id == sqlData.guildSQL.id }
        val gameInfo = LeagueMainObject.catchActiveGame(it.LOL_puuid)
        if (gameInfo != null) {
            gameInfo.participants.forEach { part ->
                val tmpLol = R2DBC.getLOLs { tbl_lols.LOL_puuid eq part.puuid }.firstOrNull()
                if (tmpLol != null) {
                    val tmpKordLOL = sqlData.getKORDLOL().find { kl -> kl.LOL_id == tmpLol.id }
                    arrayActives.add(LolActiveGame(
                        lol = tmpLol,
                        kordlol = tmpKordLOL,
                        part = part,
                        matchId = gameInfo.gameId)
                    )
                } else {
                    arrayActives.add(LolActiveGame(
                        part = part,
                        matchId = gameInfo.gameId)
                    )
                }
            }

            val mainDataList1 = (arrayActives.map { dat ->
                if (dat.kordlol != null) "**" + dat.part.riotId + " / " + LeagueMainObject.catchHeroForId(dat.part.championId)?.name + " / " + dat.part.teamId + "**"
                else dat.part.riotId + " / " + LeagueMainObject.catchHeroForId(dat.part.championId)?.name + " / " + dat.part.teamId
            })
            val mainDataList2 = (arrayActives.map { dat -> gameInfo.gameMode })

            val message = channel.createMessage {
                content = "Match game ID: ${gameInfo.gameId}"
                embed {
                    field {
                        name = "User/Hero/Team"
                        value = mainDataList1.joinToString(separator = "\n")
                        inline = true
                    }
                    field {
                        name = "Match"
                        value = mainDataList2.joinToString(separator = "\n")
                        inline = true
                    }
                }
            }

            if (kordlol?.realtime_match_message != message.id.value.toString()) {
                kordlol?.realtime_match_message = message.id.value.toString()
                kordlol?.update()
            }

        } else {
            if (kordlol?.realtime_match_message != "") {
                kordlol?.realtime_match_message = ""
                kordlol?.update()
            }
        }
        arrayActives.clear()
    }
}

suspend fun showLeagueHistory(sqlData: SQLData_R2DBC) {

    sqlData.dataKORDLOL.reset()
    sqlData.dataKORD.reset()

    sqlData.dataSavedParticipants.clear()
    sqlData.dataMMR.clear()

    sqlData.isNeedUpdateDatas = false

    sqlData.onCalculateTimer()

//    launch {
//        showRealtimeHistory(sqlData)
//    }.join()

    launch {
        val checkMatches = ArrayList<String>()
        sqlData.dataSavedLOL.get(true).forEach {
            if (it.LOL_puuid == "") return@forEach
            LeagueMainObject.catchMatchID(it.LOL_puuid, it.getCorrectName(), 0, 50).forEach ff@{ matchId ->
                if (!checkMatches.contains(matchId)) checkMatches.add(matchId)
            }
        }
        val listChecked = sqlData.getNewMatches(checkMatches)
        listChecked.sortBy { it }
        listChecked.forEach { newMatch ->
            LeagueMainObject.catchMatch(newMatch)?.let { match ->
                sqlData.addMatch(match)
            }
        }
    }.join()

    val channelText: TextChannel = sqlData.guild.getChannelOf<TextChannel>(Snowflake(sqlData.guildSQL.botChannelId))
    launch {
        //Таблица Главная - ID никнейм серияпобед
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdMain, {
            editMessageMainDataContent(it, sqlData, false)
        }) {
            createMessageMainData(channelText, sqlData)
        }
        //Таблица ММР - все про ММР арама
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdArammmr, {
            editMessageAramMMRDataContent(it, sqlData, false)
        }) {
            createMessageAramMMRData(channelText, sqlData)
        }
        //Таблица по играм\винрейту\сериям убийств
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdGlobalStatisticData, {
            editMessageGlobalStatisticContent(it, sqlData, false)
        }) {
            createMessageGlobalStatistic(channelText, sqlData)
        }
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdMasteries, {
            editMessageMasteriesContent(it, sqlData, false)
        }) {
            createMessageMasteries(channelText, sqlData)
        }
    }.join()
}

suspend fun editMessageGlobal(channelText: TextChannel, messageId: String, editBody: suspend (UserMessageModifyBuilder) -> Unit, createBody: suspend () -> Unit) {
    if (messageId.isBlank()) {
        createBody.invoke()
    } else {
        val message = channelText.getMessageOrNull(Snowflake(messageId))
        if (message != null) message.edit { editBody.invoke(this) }
        else createBody.invoke()
    }
}

suspend fun createMessageMainData(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message MainData")
    channelText.getMessage(message.id).edit { editMessageMainDataContent(this, sqlData, true) }

    sqlData.guildSQL.messageIdMain = message.id.value.toString()
    sqlData.guildSQL.update()
}

suspend fun createMessageGlobalStatistic(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message GlobalStatistic")
    channelText.getMessage(message.id).edit {
        editMessageGlobalStatisticContent(this, sqlData, true)
    }

    sqlData.guildSQL.messageIdGlobalStatisticData = message.id.value.toString()
    sqlData.guildSQL.update()
}

suspend fun createMessageAramMMRData(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message AramMMR")
    channelText.getMessage(message.id).edit { editMessageAramMMRDataContent(this, sqlData, true) }

    sqlData.guildSQL.messageIdArammmr = message.id.value.toString()
    sqlData.guildSQL.update()
}

suspend fun createMessageMasteries(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message Masteries")
    channelText.getMessage(message.id).edit {
        editMessageMasteriesContent(this, sqlData, true)
    }

    sqlData.guildSQL.messageIdMasteries = message.id.value.toString()
    sqlData.guildSQL.update()
}

suspend fun editMessageGlobalStatisticContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC, afterCreating: Boolean) {

    builder.content = "**Статистика Матчей**\nОбновлено: ${TimeStamp.now()}\n"

    if (!afterCreating) {
        if (!sqlData.isNeedUpdateDatas) return
    }

    val charStr = " / "
    val savedParts = sqlData.getSavedParticipants()

    savedParts.sortBy { it.kordLOL?.showCode }

    val mainDataList1 = (savedParts.map { formatInt(it.kordLOL?.showCode, 2) + "| " + formatInt(it.games, 3) + charStr + formatInt(it.win, 3) + charStr + formatInt(((it.win.toDouble() / it.games.toDouble()) * 100).toInt(), 2) + "%" })
    val mainDataList2 = (savedParts.map {  it.kill.toFormatK() + charStr + formatInt(it.kill3, 3) + charStr + formatInt(it.kill4, 3) + charStr + formatInt(it.kill5, 2) })

    builder.embed {
        field {
            name = "Game/Win/WinRate"
            value = mainDataList1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Kill/Triple/Quadra/Penta"
            value = mainDataList2.joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessageGlobalStatisticContent] completed")
}

data class DataMasteriesChampion(
    val championId: Int,
    val championPoints: Int
)

suspend fun editMessageMasteriesContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC, afterCreating: Boolean) {

    if (!afterCreating) {
        if (sqlData.guildSQL.messageIdMasteriesUpdated.toDate().isCurrentDay()) return
    }

    builder.content = "**Мастерство чемпионов (ТОП 3)**\nОбновлено: ${TimeStamp.now()}\n"

    val savedPartsHash = HashMap<KORDLOLs, ArrayList<DataMasteriesChampion>>()
    sqlData.dataSavedLOL.get().forEach {
        if (it.LOL_puuid.isEmpty()) return@forEach
        val kordlol = sqlData.getKORDLOL().find { kl -> kl.LOL_id == it.id } ?: return@forEach
        savedPartsHash[kordlol] = ArrayList()
        LeagueMainObject.catchChampionMasteries(it.LOL_puuid).forEach { dto ->
            savedPartsHash[kordlol]!!.add(DataMasteriesChampion(dto.championId, dto.championPoints))
        }
    }

    val savedParts = savedPartsHash.map { it.key to it.value }.sortedBy { it.first.showCode }.toMap()

    val charStr = " / "
    val mainDataList1 = (savedParts.map { formatInt(it.key.showCode, 2) + "| " + LeagueMainObject.catchHeroForId(it.value[0].championId)?.name.toMaxSymbols(8, "..") + charStr + LeagueMainObject.catchHeroForId(it.value[1].championId)?.name.toMaxSymbols(8, "..") + charStr + LeagueMainObject.catchHeroForId(it.value[2].championId)?.name.toMaxSymbols(8, "..") })
    val mainDataList2 = (savedParts.map { it.value[0].championPoints.toFormatK() + charStr + it.value[1].championPoints.toFormatK() + charStr + it.value[2].championPoints.toFormatK() })

    builder.embed {
        field {
            name = "Champions"
            value = mainDataList1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Points"
            value = mainDataList2.joinToString(separator = "\n")
            inline = true
        }
    }

    sqlData.guildSQL.messageIdMasteriesUpdated = System.currentTimeMillis()
    sqlData.guildSQL.update()
    printLog(sqlData.guild, "[editMessageGlobalStatisticContent] completed")
}

suspend fun editMessageMainDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC, afterCreating: Boolean) {

    builder.content = "**Статистика Главная**\nОбновлено: ${TimeStamp.now()}\n"

    if (!afterCreating) {
        if (!sqlData.isNeedUpdateDatas) return
    }

    val data = sqlData.getKORDLOL()
    data.sortBy { it.showCode }

    val charStr = "/"
    val wStreak = sqlData.getWinStreak()

    val mainDataList1 = (data.map { formatInt(it.showCode, 2) + charStr + it.asUser(sqlData.guild, sqlData).lowDescriptor() })
    val mainDataList2 = (data.map { sqlData.getLOL(it.LOL_id)?.getCorrectName() })
    val mainDataList3 = (data.map { wStreak[it.LOL_id] })
    builder.embed {
        field {
            name = "ID/User"
            value = mainDataList1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Nickname"
            value = mainDataList2.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "WinStreak"
            value = mainDataList3.joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessageMainDataContent] completed")
}

suspend fun editMessageAramMMRDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC, afterCreating: Boolean) {

    builder.content = "**Статистика ММР**\nОбновлено: ${TimeStamp.now()}\n"

    if (!afterCreating) {
        if (!sqlData.isNeedUpdateDatas) return
    }

    val charStr = " / "
    val aramData = sqlData.getArrayAramMMRData()

    aramData.sortBy { it.kordLOL?.showCode }

    val mainDataList1 = (aramData.map {
        if (it.match_id == it.last_match_id) "**" + formatInt(it.kordLOL?.showCode, 2) + "| " + EnumMMRRank.getMMRRank(it.mmr_aram).nameRank + "**"
        else formatInt(it.kordLOL?.showCode, 2) + "| " + EnumMMRRank.getMMRRank(it.mmr_aram).nameRank
    })
    val mainDataList2 = (aramData.map {
        if (it.match_id == it.last_match_id) "**" + it.mmr_aram + charStr + it.mmr_aram_saved + charStr + (it.mmr_aram / it.games).toFormat(2) + "**"
        else it.mmr_aram.toString() + charStr + it.mmr_aram_saved + charStr + (it.mmr_aram / it.games).toFormat(2)
    })
    val mainDataList3 = (aramData.map {
        if (it.match_id == it.last_match_id) "**" + LeagueMainObject.catchHeroForId(it.champion_id)?.name + charStr + it.mmr + " " + it.mvp_lvp_info + "**"
        else LeagueMainObject.catchHeroForId(it.champion_id)?.name + charStr + it.mmr + " " + it.mvp_lvp_info
    })


    builder.embed {
        field {
            name = "ARAM Rank"
            value = mainDataList1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "MMR/Bonus/AVG"
            value = mainDataList2.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "LastGame/MMR"
            value = mainDataList3.joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessageAramMMRDataContent] completed")
}