package ru.descend.bot

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
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
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.operator.desc
import reactor.core.publisher.Hooks
import ru.descend.bot.datas.Toppartisipants
import ru.descend.bot.datas.getData
import ru.descend.bot.datas.getDataOne
import ru.descend.bot.datas.getSize
import ru.descend.bot.enums.EnumMMRRank
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Matches.Companion.tbl_matches
import ru.descend.bot.datas.update
import ru.descend.bot.datas.isCurrentDay
import ru.descend.bot.datas.toDate
import ru.descend.bot.lolapi.dto.championMasteryDto.ChampionMasteryDtoItem
import ru.descend.bot.postgre.r2dbc.model.Heroes
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew.Companion.tbl_participantsnew
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
            printLog("Exception '${exception::class.simpleName}': ${exception.message}")
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
                timerMainInformation(it, (121).seconds)
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
                garbaceCollect()
                printMemoryUsage()
            } else {
                skipped = true
            }
        }
        delay(duration)
    }
}

private suspend fun firstInitialize() {
    R2DBC.initialize()
    connectLogger()
}

suspend fun removeMessage(guild: Guild) {
    if (mapMainData[guild]!!.guildSQL.botChannelId.isNotEmpty()){
        guild.getChannelOf<TextChannel>(Snowflake(mapMainData[guild]!!.guildSQL.botChannelId)).messages.collect {
            if (it.id.value.toString() in listOf(mapMainData[guild]!!.guildSQL.messageIdGlobalStatisticData, mapMainData[guild]!!.guildSQL.messageIdMain, mapMainData[guild]!!.guildSQL.messageIdArammmr, mapMainData[guild]!!.guildSQL.messageIdMasteries, mapMainData[guild]!!.guildSQL.messageIdTop)) {
                Unit
            } else {
                it.delete()
            }
        }
    }
}

var globalLOLRequests = 0
var statusLOLRequests = 0

//suspend fun showRealtimeHistory(sqlData: SQLData_R2DBC) {
//    val channel = sqlData.guild.getChannelOf<TextChannel>(Snowflake(1225762638735085620))
//    val arrayActives = ArrayList<LolActiveGame>()
//    sqlData.dataSavedLOL.get().forEach {
//        if (it.LOL_puuid.isEmpty()) return@forEach
//        if (arrayActives.find { ara -> ara.part.puuid == it.LOL_puuid } != null) return@forEach
//        val kordlol = sqlData.getKORDLOL().firstOrNull { kd -> kd.LOL_id == it.id && kd.guild_id == sqlData.guildSQL.id }
//        val gameInfo = LeagueMainObject.catchActiveGame(it.LOL_puuid)
//        if (gameInfo != null) {
//            gameInfo.participants.forEach { part ->
//                val tmpLol = LOLs().getDataOne(declaration = {tbl_lols.LOL_puuid eq part.puuid})
//                if (tmpLol != null) {
//                    val tmpKordLOL = sqlData.getKORDLOL().find { kl -> kl.LOL_id == tmpLol.id }
//                    arrayActives.add(LolActiveGame(
//                        lol = tmpLol,
//                        kordlol = tmpKordLOL,
//                        part = part,
//                        matchId = gameInfo.gameId)
//                    )
//                } else {
//                    arrayActives.add(LolActiveGame(
//                        part = part,
//                        matchId = gameInfo.gameId)
//                    )
//                }
//            }
//
//            val mainDataList1 = (arrayActives.map { dat ->
//                if (dat.kordlol != null) "**" + dat.part.riotId + " / " + R2DBC.getHeroFromKey(dat.part.championId.toString())?.nameRU + " / " + dat.part.teamId + "**"
//                else dat.part.riotId + " / " + R2DBC.getHeroFromKey(dat.part.championId.toString())?.nameRU + " / " + dat.part.teamId
//            })
//            val mainDataList2 = (arrayActives.map { gameInfo.gameMode })
//
//            val message = channel.createMessage {
//                content = "Match game ID: ${gameInfo.gameId}"
//                embed {
//                    field {
//                        name = "User/Hero/Team"
//                        value = mainDataList1.joinToString(separator = "\n")
//                        inline = true
//                    }
//                    field {
//                        name = "Match"
//                        value = mainDataList2.joinToString(separator = "\n")
//                        inline = true
//                    }
//                }
//            }
//
//            if (kordlol?.realtime_match_message != message.id.value.toString()) {
//                kordlol?.realtime_match_message = message.id.value.toString()
//                kordlol?.update()
//            }
//
//        } else {
//            if (kordlol?.realtime_match_message != "") {
//                kordlol?.realtime_match_message = ""
//                kordlol?.update()
//            }
//        }
//        arrayActives.clear()
//    }
//}

private suspend fun getLastLOLs(sqlData: SQLData_R2DBC, region: String, size: Int) : List<LOLs> {
    return measureBlock(EnumMeasures.METHOD, "getLastLOLs") {
        val query = QueryDsl
            .from(tbl_lols)
            .leftJoin(tbl_participantsnew) { tbl_participantsnew.LOLperson_id eq tbl_lols.id }
//            .innerJoin(tbl_matches) { tbl_matches.id eq tbl_participantsnew.match_id }
            .where {
                tbl_lols.last_loaded.less(sqlData.olderDateLong)
                tbl_lols.LOL_region.eq(region)
//                tbl_lols.LOL_riotIdName.notEq("null")
            }
            .orderBy(listOf(tbl_participantsnew.matchDateEnd.desc()))
            .limit(size)
            .selectAsEntity(tbl_lols)

        R2DBC.runQuery { query }
    }
}
/**
 * Загружаем 10 матчей по последнему пользователю по которому еще не было загрузки матчей
 */
suspend fun loadingLastMatches(sqlData: SQLData_R2DBC) {
    if (sqlData.atomicIntLoaded.get() >= (98 - LOAD_MATCHES_IN_USER)) return

    val resultedList = ArrayList<LOLs>()
    LOLs().getDataOne({ tbl_lols.LOL_region eq "EUW1" ; tbl_lols.last_loaded.less(sqlData.olderDateLong) }, tbl_lols.id.desc())?.let { resultedList.add(it) }
    resultedList.addAll(getLastLOLs(sqlData, "RU", LAST_UNDEFINED_USERS_FOR_LOAD))
    resultedList.forEach {lastLOLObj ->
        if (sqlData.atomicIntLoaded.get() >= (98 - LOAD_MATCHES_IN_USER)) return@forEach
        printLog("[loadingLastMatches::updated] LOL id: ${lastLOLObj.id} ${lastLOLObj.getCorrectName()} before: ${lastLOLObj.last_loaded}(${lastLOLObj.last_loaded.toFormatDate()}) new: ${sqlData.currentDateLong}(${sqlData.currentDateLong.toFormatDate()}) checked: ${sqlData.olderDateLong}(${sqlData.olderDateLong.toFormatDate()})")
        lastLOLObj.last_loaded = sqlData.currentDateLong
        lastLOLObj.update()
        sqlData.loadMatches(listOf(lastLOLObj), LOAD_MATCHES_IN_USER, false)
    }
}

suspend fun showLeagueHistory(sqlData: SQLData_R2DBC) {
    sqlData.onCalculateTimer()

    launch {
        sqlData.updatesBeforeLoadUsersMatch++
        val arraySaveds = sqlData.dataSavedLOL.get()
        if (sqlData.updatesBeforeLoadUsersMatch == 1) {
            sqlData.loadMatches(arraySaveds, LOAD_SAVED_USER_MATCHES, true)
        } else if (sqlData.updatesBeforeLoadUsersMatch >= EVERY_N_TICK_LOAD_MATCH) {
            sqlData.updatesBeforeLoadUsersMatch = 0
        }
        printLog("[showLeagueHistory loaded: ${sqlData.atomicIntLoaded.get()}][updates: ${sqlData.updatesBeforeLoadUsersMatch}]")
        loadingLastMatches(sqlData)

        sqlData.textNewMatches.getAllText().forEach {str ->
            sqlData.sendMessage(sqlData.guildSQL.messageIdDebug, str)
            delay(1000)
        }
        sqlData.clearTempData()
    }.join()

    val channelText: TextChannel = sqlData.guild.getChannelOf<TextChannel>(Snowflake(sqlData.guildSQL.botChannelId))
    launch {
        //Таблица Главная - ID никнейм серияпобед
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdMain, "MessageMainDataContent", {
            editMessageMainDataContent(it, sqlData)
        }) {
            createMessageMainData(channelText, sqlData)
        }

        //Таблица ММР - все про ММР арама
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdArammmr, "MessageAramMMRDataContent", {
            editMessageAramMMRDataContent(it, sqlData)
        }) {
            createMessageAramMMRData(channelText, sqlData)
        }

        //Таблица по играм\винрейту\сериям убийств
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdGlobalStatisticData, "MessageGlobalStatisticContent", {
            editMessageGlobalStatisticContent(it, sqlData)
        }) {
            createMessageGlobalStatistic(channelText, sqlData)
        }

        //Таблица по Мастерству ТОП3 чемпионов каждого игрока
        if (isNeedUpdateMasteries(channelText, sqlData)) {
            editMessageGlobal(channelText, sqlData.guildSQL.messageIdMasteries, "MessageMasteriesContent", {
                editMessageMasteriesContent(it, sqlData)
            }) {
                createMessageMasteries(channelText, sqlData)
            }
        }

        //Таблица по ТОП чемпионам сервера
        if (isNeedUpdateTop(channelText, sqlData)) {
            editMessageGlobal(channelText, sqlData.guildSQL.messageIdTop, "MessageTopContent", {
                editMessageTopContent(it, sqlData)
            }) {
                createMessageTop(channelText, sqlData)
            }
        }
    }.join()

    sqlData.dataKORDLOL.reset()
    sqlData.dataKORD.reset()
    sqlData.dataSavedLOL.reset()
    sqlData.dataSavedParticipants.clear()
}

suspend fun isNeedUpdateTop(channelText: TextChannel, sqlData: SQLData_R2DBC) : Boolean {
    channelText.getMessageOrNull(Snowflake(sqlData.guildSQL.messageIdTop)) ?: return true
    if (!sqlData.isNeedUpdateDays) {
        if (sqlData.guildSQL.messageIdTopUpdated.toDate().isCurrentDay()) return false
    }
    return true
}

suspend fun isNeedUpdateMasteries(channelText: TextChannel, sqlData: SQLData_R2DBC) : Boolean {
    channelText.getMessageOrNull(Snowflake(sqlData.guildSQL.messageIdMasteries)) ?: return true
    if (!sqlData.isNeedUpdateDays) {
        if (sqlData.guildSQL.messageIdMasteriesUpdated.toDate().isCurrentDay()) return false
    }
    return true
}

suspend fun editMessageGlobal(channelText: TextChannel, messageId: String, measuredText: String, editBody: suspend (UserMessageModifyBuilder) -> Unit, createBody: suspend () -> Unit) {
    measureBlock(EnumMeasures.BLOCK, measuredText) {
        if (messageId.isBlank()) {
            createBody.invoke()
        } else {
            val message = channelText.getMessageOrNull(Snowflake(messageId))
            message?.edit { editBody.invoke(this) } ?: createBody.invoke()
        }
    }
}

suspend fun createMessageMainData(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message MainData")
    channelText.getMessage(message.id).edit { editMessageMainDataContent(this, sqlData) }

    sqlData.guildSQL.messageIdMain = message.id.value.toString()
    sqlData.guildSQL.update()
}

suspend fun createMessageGlobalStatistic(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message GlobalStatistic")
    channelText.getMessage(message.id).edit {
        editMessageGlobalStatisticContent(this, sqlData)
    }

    sqlData.guildSQL.messageIdGlobalStatisticData = message.id.value.toString()
    sqlData.guildSQL.update()
}

suspend fun createMessageAramMMRData(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message AramMMR")
    channelText.getMessage(message.id).edit { editMessageAramMMRDataContent(this, sqlData) }

    sqlData.guildSQL.messageIdArammmr = message.id.value.toString()
    sqlData.guildSQL.update()
}

suspend fun createMessageMasteries(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message Masteries")
    channelText.getMessage(message.id).edit {
        editMessageMasteriesContent(this, sqlData)
    }

    sqlData.guildSQL.messageIdMasteries = message.id.value.toString()
    sqlData.guildSQL.update()
}

suspend fun createMessageTop(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message Top")
    channelText.getMessage(message.id).edit {
        editMessageTopContent(this, sqlData)
    }

    sqlData.guildSQL.messageIdTop = message.id.value.toString()
    sqlData.guildSQL.update()
}

suspend fun editMessageGlobalStatisticContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC) {

    builder.content = "**Статистика Матчей**\nОбновлено: ${TimeStamp.now()}\n"

    val charStr = " / "
    val savedParts = sqlData.getSavedParticipants()

    savedParts.sortBy { it.kordLOL?.showCode }

    val mainDataList1 = (savedParts.map { formatInt(it.kordLOL?.showCode, 2) + "| " + formatInt(it.games, 3) + charStr + formatInt(it.win, 3) + charStr + ((it.win.toDouble() / it.games.toDouble()) * 100.0).to1Digits() + "%" })
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
}

suspend fun editMessageTopContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC){
    if (sqlData.isNeedUpdateDays) sqlData.isNeedUpdateDays = false

    builder.content = "**ТОП сервера по параметрам (за матч)**\nОбновлено: ${TimeStamp.now()}\n"

    sqlData.generateFact()

    val query = QueryDsl
        .from(tbl_participantsnew)
        .innerJoin(tbl_matches) { tbl_matches.id eq tbl_participantsnew.match_id }
        .innerJoin(KORDLOLs.tbl_kordlols) { KORDLOLs.tbl_kordlols.LOL_id eq tbl_participantsnew.LOLperson_id }
        .where { tbl_matches.matchMode.inList(listOf("ARAM", "CLASSIC")) ; tbl_matches.bots eq false ; tbl_matches.surrender eq false ; tbl_matches.aborted eq false }
        .orderBy(tbl_participantsnew.id)
        .selectAsEntity(tbl_participantsnew)

    val statClass = Toppartisipants()
    val result = R2DBC.runQuery { query }
    result.forEach {
        statClass.calculateField(it, "Убийств", it.kills.toDouble())
        statClass.calculateField(it, "Смертей", it.deaths.toDouble())
        statClass.calculateField(it, "Ассистов", it.assists.toDouble())
        statClass.calculateField(it, "KDA", it.kda)
        statClass.calculateField(it, "Урон в минуту", it.damagePerMinute)
        statClass.calculateField(it, "Урона строениям", it.damageDealtToBuildings.toDouble())
        statClass.calculateField(it, "Урона поглощено", it.damageSelfMitigated.toDouble())
        statClass.calculateField(it, "Провёл в контроле (сек)", it.timeCCingOthers.toDouble())
        statClass.calculateField(it, "Наложил контроля (сек)", it.totalTimeCCDealt.toDouble())
        statClass.calculateField(it, "Обездвиживаний нанесено (сек)", it.enemyChampionImmobilizations.toDouble())
        statClass.calculateField(it, "Получено золота", it.goldEarned.toDouble())
        statClass.calculateField(it, "Критический удар", it.largestCriticalStrike.toDouble())
        statClass.calculateField(it, "Магического урона чемпионам", it.magicDamageDealtToChampions.toDouble())
        statClass.calculateField(it, "Физического урона чемпионам", it.physicalDamageDealtToChampions.toDouble())
        statClass.calculateField(it, "Чистого урона чемпионам", it.trueDamageDealtToChampions.toDouble())
        statClass.calculateField(it, "Убито миньонов", it.totalMinionsKilled.toDouble())
        statClass.calculateField(it, "Использовано заклинаний", it.calcSkillShots().toDouble())
        statClass.calculateField(it, "Уклонений от заклинаний", it.skillshotsDodged.toDouble())
        statClass.calculateField(it, "Попаданий заклинаниями", it.skillshotsHit.toDouble())
        statClass.calculateField(it, "Попаданий снежками", it.snowballsHit.toDouble())
        statClass.calculateField(it, "Наложено щитов союзникам", it.totalDamageShieldedOnTeammates.toDouble())
        statClass.calculateField(it, "Получено урона", it.totalDamageTaken.toDouble())
        statClass.calculateField(it, "Нанесено урона чемпионам", it.totalDamageDealtToChampions.toDouble())
        statClass.calculateField(it, "Лечение союзников", it.totalHealsOnTeammates.toDouble())
        statClass.calculateField(it, "Времени жизни (сек)", it.longestTimeSpentLiving.toDouble())
        statClass.calculateField(it, "Времени на экране смерти (сек)", it.totalTimeSpentDead.toDouble())
    }

    var resultText = ""
    statClass.getResults(sqlData).forEach {
        resultText += "* $it\n"
    }
    builder.content += resultText

    sqlData.guildSQL.messageIdTopUpdated = System.currentTimeMillis()
    sqlData.guildSQL.update()
}

suspend fun editMessageMasteriesContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC) {
    if (sqlData.isNeedUpdateDays) sqlData.isNeedUpdateDays = false

    builder.content = "**Мастерство чемпионов (ТОП 3)**\nОбновлено: ${TimeStamp.now()}\n"
    val charStr = " / "

    val savedPartsHash = HashMap<KORDLOLs, ArrayList<ChampionMasteryDtoItem>>()
    sqlData.dataSavedLOL.get().forEach {
        if (it.LOL_puuid.isEmpty()) return@forEach
        val kordlol = sqlData.getKORDLOL().find { kl -> kl.LOL_id == it.id } ?: return@forEach
        savedPartsHash[kordlol] = ArrayList()
        LeagueMainObject.catchChampionMasteries(it.LOL_puuid, it.LOL_region).forEach { dto ->
            savedPartsHash[kordlol]!!.add(dto)
        }
        while (savedPartsHash[kordlol]!!.size < 3) {
            savedPartsHash[kordlol]!!.add(ChampionMasteryDtoItem(0, 0, 0, 0, 0, false, 0, "", "", 0))
        }
    }

    val savedParts = savedPartsHash.map { it.key to it.value }.sortedBy { it.first.showCode }.toMap()

    val mainDataList1 = (savedParts.map { formatInt(it.key.showCode, 2) + "| " + R2DBC.getHeroFromKey(it.value[0].championId.toString())?.nameRU.toMaxSymbols(8, "..") + charStr + R2DBC.getHeroFromKey(it.value[1].championId.toString())?.nameRU.toMaxSymbols(8, "..") + charStr + R2DBC.getHeroFromKey(it.value[2].championId.toString())?.nameRU.toMaxSymbols(8, "..") })
    val mainDataList2 = (savedParts.map {
        it.value[0].championLevel.toString() + " (" + it.value[0].championPoints.toFormatK() + ")" + charStr +
        it.value[1].championLevel.toString() + " (" + it.value[1].championPoints.toFormatK() + ")" + charStr +
        it.value[2].championLevel.toString() + " (" + it.value[2].championPoints.toFormatK() + ")"
    })

    builder.embed {
        field {
            name = "Champions"
            value = mainDataList1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Level/Points"
            value = mainDataList2.joinToString(separator = "\n")
            inline = true
        }
    }

    sqlData.guildSQL.messageIdMasteriesUpdated = System.currentTimeMillis()
    sqlData.guildSQL.update()
}

suspend fun editMessageMainDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC) {

    val sizeMatches = Matches().getSize()
    val sizeLOLs = LOLs().getSize()
    val sizeParticipants = ParticipantsNew().getSize()
    val sizeHeroes = Heroes().getSize()

    var contentText = "**Статистика Главная**\nОбновлено: ${TimeStamp.now()}\n"
    contentText += "* Матчей: $sizeMatches (CLASSIC:${Matches().getSize { tbl_matches.matchMode.eq("CLASSIC") }} ARAM:${Matches().getSize { tbl_matches.matchMode.eq("ARAM") }})\n"
    contentText += "* Игроков: $sizeLOLs (RU:${LOLs().getSize { tbl_lols.LOL_region.eq("RU") }} EUW1:${LOLs().getSize { tbl_lols.LOL_region.eq("EUW1") }})\n"
    contentText += "* Данных (строк): ~${sizeParticipants + sizeMatches + sizeLOLs + sizeHeroes}\n"
    contentText += "* Чемпионов: $sizeHeroes\n"
    contentText += "* Версия игры: ${LeagueMainObject.LOL_VERSION}\n"
    builder.content = contentText

    val data = sqlData.getKORDLOL()
    data.sortBy { it.showCode }

    val charStr = "/"
    val wStreak = sqlData.getWinStreak()
    val curLOLlist = LOLs().getData({ tbl_lols.id.inList(data.map { dat -> dat.LOL_id }) })

    val mainDataList1 = (data.map { formatInt(it.showCode, 2) + charStr + it.asUser(sqlData).lowDescriptor() })
    val mainDataList2 = (data.map { curLOLlist.find { lol -> lol.id == it.LOL_id }?.getCorrectNameWithTag()?.toMaxSymbols(18, "..") })
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
}

suspend fun editMessageAramMMRDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC) {

    builder.content = "**Статистика ММР**\nОбновлено: ${TimeStamp.now()}\n"

    val charStr = " / "
    val aramData = sqlData.getArrayAramMMRData()

    aramData.sortBy { it.kordLOL?.showCode }

    val mainDataList1 = (aramData.map {
        val textBold = if (it.bold) "**" else ""
        textBold + formatInt(it.kordLOL?.showCode, 2) + "| " + EnumMMRRank.getMMRRank(it.mmr_aram).nameRank + textBold
    })
    val mainDataList2 = (aramData.map {
        val textBold = if (it.bold) "**" else ""
        textBold + it.mmr_aram.toString() + charStr + it.mmr_aram_saved + textBold
    })
    val mainDataList3 = (aramData.map {
        val textBold = if (it.bold) "**" else ""
        textBold + R2DBC.getHeroFromKey(it.champion_id.toString())?.nameRU + charStr + it.mmr + " " + it.mvp_lvp_info + textBold
    })


    builder.embed {
        field {
            name = "ARAM Rank"
            value = mainDataList1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "MMR/Bonus"
            value = mainDataList2.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "LastGame/MMR"
            value = mainDataList3.joinToString(separator = "\n")
            inline = true
        }
    }
}

private fun connectLogger() {
    Hooks.onErrorDropped {
        writeLog("[HOOKS_ERROR] ${it.localizedMessage}")
        it.stackTrace.forEach { trace ->
            writeLog("\t[HOOKS_ERROR] $trace")
        }
    }
}