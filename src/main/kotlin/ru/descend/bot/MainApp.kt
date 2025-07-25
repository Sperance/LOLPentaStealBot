package ru.descend.bot

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.TextChannel
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.embed
import dev.kord.rest.builder.message.modify.UserMessageModifyBuilder
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.jakejmattson.discordkt.dsl.bot
import me.jakejmattson.discordkt.util.TimeStamp
import org.komapper.core.dsl.QueryDsl
import ru.descend.bot.datas.Toplols
import ru.descend.bot.datas.Toppartisipants
import ru.descend.bot.datas.getData
import ru.descend.bot.datas.getSize
import ru.descend.bot.datas.isCurrentDay
import ru.descend.bot.datas.toDate
import ru.descend.bot.datas.update
import ru.descend.bot.enums.EnumARAMRank
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.dto.championMasteryDto.ChampionMasteryDtoItem
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.calculating.Calc_LoadMAtches
import ru.descend.bot.postgre.calculating.PlayerRank
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Matches.Companion.tbl_matches
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew.Companion.tbl_participantsnew
import ru.descend.kotlintelegrambot.Bot
import ru.descend.kotlintelegrambot.dispatch
import ru.descend.kotlintelegrambot.dispatcher.telegramError
import ru.descend.kotlintelegrambot.handlers.handleButtons
import ru.descend.kotlintelegrambot.handlers.handleCommands
import ru.descend.kotlintelegrambot.handlers.handleMMRstat
import ru.descend.kotlintelegrambot.handlers.handleOthers
import ru.descend.kotlintelegrambot.handlers.last_date_loaded_discord
import ru.descend.kotlintelegrambot.handlers.last_date_loaded_matches
import ru.descend.kotlintelegrambot.handlers.stopTelegramBot
import java.awt.Color
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val discordScope = CoroutineScope(Dispatchers.IO)

fun main() {
    printLog("server start")

    val scope = CoroutineScope(Dispatchers.IO)

    scope.launch {
        printLog("startDiscordBot")
        startDiscordBot()
    }

    scope.launch {
        printLog("startLoadingMatches")
        startLoadingMatches()
    }

    scope.launch {
        printLog("startTelegramBot")
        startTelegramBot()
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        stopTelegramBot()
        telegram_bot.stopPolling()
        scope.cancel("server shutdown")
        printLog("server shutdown")
    })

    Thread.currentThread().join()
}

lateinit var sqlData: SQLData_R2DBC
val atomicIntLoaded = AtomicInteger()
var sql_data_initialized = false

private fun startLoadingMatches() = launch {
    delay((30).seconds)
    while (true) {
        if (sql_data_initialized) {
            val kordLol_lol_id = KORDLOLs().getData().map { it.LOL_id }
            val savedLols = R2DBC.runQuery(QueryDsl.from(tbl_lols).where { tbl_lols.id.inList(kordLol_lol_id) })
            val loaderMatches = Calc_LoadMAtches()
            loaderMatches.loadMatches(savedLols, LOAD_SAVED_USER_MATCHES)
            loaderMatches.clearTempData()
            last_date_loaded_matches = Date()
        } else {
            printLog("[sql_data not initialized]")
        }
        delay((2).minutes)
    }
}

@OptIn(PrivilegedIntent::class)
fun startDiscordBot() {
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
            intents = Intents(Intent.GuildMembers, Intent.GuildModeration, Intent.Guilds)
            theme = Color(0x00B92F)
            defaultPermissions = Permissions(Permission.UseApplicationCommands)
        }
        onException {
            printLog("Exception '${exception::class.simpleName}': ${exception.message}")
        }
        presence {
            this.status = PresenceStatus.Online
            playing("ARAM")
        }
        onStart {
            R2DBC.initialize()
            kord.guilds.collect {
                if (it.name == "АрамоЛолево") {
                    val guilds = Guilds().getData().first()
                    sqlData = SQLData_R2DBC(it, guilds)
                    sqlData.initialize()
                    sql_data_initialized = true
                    removeMessage()

                    timerRequestReset((2).minutes)
                    timerMainInformation((121).seconds)
                }
            }
        }
    }
}

lateinit var telegram_bot: Bot

private fun startTelegramBot() {
    telegram_bot = ru.descend.kotlintelegrambot.bot {
        timeout = 120
        dispatch {
            handleMMRstat()

            telegramError {
//                telegram_bot.deleteWebhook()
                printLog("Telegram error: " + error.getErrorMessage())
            }
        }
    }
    telegram_bot.startPolling()
    telegram_bot.deleteWebhook()
}

fun timerRequestReset(duration: Duration) = launch {
    while (true) {
        globalLOLRequests = 0
        delay(duration)
    }
}

suspend fun timerMainInformation(duration: Duration) {
    while (true) {
        printLog("[showLeagueHistory::${sqlData.guildSQL.botChannelId}]")
        if (sqlData.guildSQL.botChannelId.isNotEmpty()) {
            last_date_loaded_discord = Date()
            showLeagueHistory(sqlData)
            garbaceCollect()
            printMemoryUsage(" [TELEGRAM: $telegram_bot]")
        }
        delay(duration)
    }
}

suspend fun removeMessage() {
    if (sqlData.guildSQL.botChannelId.isNotEmpty()){
        sqlData.guild.getChannelOf<TextChannel>(Snowflake(sqlData.guildSQL.botChannelId)).messages.collect {
            if (it.id.value.toString() in listOf(sqlData.guildSQL.messageIdGlobalStatisticData, sqlData.guildSQL.messageIdMain, sqlData.guildSQL.messageIdArammmr, sqlData.guildSQL.messageIdMasteries, sqlData.guildSQL.messageIdTop, sqlData.guildSQL.messageIdTopLols)) {
                Unit
            } else {
                it.delete()
            }
        }
    }
}

var globalLOLRequests = 0
var statusLOLRequests = 0

suspend fun showLeagueHistory(sqlData: SQLData_R2DBC) {
    sqlData.onCalculateTimer()

    val channelText: TextChannel = sqlData.guild.getChannelOf<TextChannel>(Snowflake(sqlData.guildSQL.botChannelId))
    sqlData.dataSavedLOL.get(true)
    sqlData.dataKORDLOL.get(true)
    sqlData.dataKORD.get(true)

    //Таблица Главная - ID никнейм серияпобед
    discordScope.launch {
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdMain, "MessageMainDataContent", {
            editMessageMainDataContent(it, sqlData)
        }) {
            createMessageMainData(channelText, sqlData)
        }
    }.join()
    //Таблица ММР - все про ММР арама
    discordScope.launch {
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdArammmr, "MessageAramMMRDataContent", {
            editMessageAramMMRDataContent(it, sqlData)
        }) {
            createMessageAramMMRData(channelText, sqlData)
        }
    }.join()
    //Таблица по играм\винрейту\сериям убийств
    discordScope.launch {
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdGlobalStatisticData, "MessageGlobalStatisticContent", {
            editMessageGlobalStatisticContent(it)
        }) {
            createMessageGlobalStatistic(channelText, sqlData)
        }
    }.join()
    //Таблица по Мастерству ТОП3 чемпионов каждого игрока
    discordScope.launch {
        if (isNeedUpdateMasteries(channelText, sqlData)) {
            editMessageGlobal(channelText, sqlData.guildSQL.messageIdMasteries, "MessageMasteriesContent", {
                editMessageMasteriesContent(it, sqlData)
            }) {
                createMessageMasteries(channelText, sqlData)
            }
        }
    }.join()
    discordScope.launch {
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdTopLols, "MessageTopLoLContent", {
            editMessageTopLolContent(it)
        }) {
            createMessageTopLol(channelText, sqlData)
        }
    }.join()
    //Таблица по ТОП чемпионам сервера
    discordScope.launch {
        if (isNeedUpdateTop(channelText, sqlData)) {
            editMessageGlobal(channelText, sqlData.guildSQL.messageIdTop, "MessageTopContent", {
                editMessageTopContent(it, sqlData)
            }) {
                createMessageTop(channelText, sqlData)
            }
        }
    }.join()

    printLog("[showLeagueHistory::completed]")

    sqlData.dataKORDLOL.clear()
    sqlData.dataKORD.clear()
    sqlData.dataSavedLOL.clear()

    sqlData.isHaveLastARAM = false
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
    if (messageId.isBlank()) {
        createBody.invoke()
    } else {
        try {
            printLog("[editMessageGlobal] 1 $measuredText")
            val message = channelText.getMessageOrNull(Snowflake(messageId))
            printLog("[editMessageGlobal] 2 $measuredText")
            message?.edit { editBody.invoke(this) } ?: createBody.invoke()
            printLog("[editMessageGlobal] 3 $measuredText")
        } catch (e: Exception) {
            printLog("[editMessageGlobal] 4 $measuredText")
            e.printStackTrace()
        } finally {
            printLog("[editMessageGlobal] 5 $measuredText")
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
        editMessageGlobalStatisticContent(this)
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

suspend fun createMessageTopLol(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message TopLol")
    channelText.getMessage(message.id).edit {
        editMessageTopLolContent(this)
    }

    sqlData.guildSQL.messageIdTopLols = message.id.value.toString()
    sqlData.guildSQL.update()
}

suspend fun editMessageGlobalStatisticContent(builder: UserMessageModifyBuilder) {
    builder.content = "**Статистика Матчей**\nОбновлено: ${TimeStamp.now()}\n"

    val charStr = " / "
    val savedParts = sqlData.dataSavedLOL.get()
    val sortedList = savedParts.sortedBy { it.show_code }

    val mainDataList1 = (sortedList.map { formatInt(it.show_code, 2) + "| " + formatInt(it.f_aram_games.toInt(), 3) + charStr + formatInt(it.f_aram_wins.toInt(), 3) + charStr + ((it.f_aram_wins / it.f_aram_games) * 100.0).to1Digits() + "%" })
    val mainDataList2 = (sortedList.map {  it.f_aram_kills.toInt().toFormatK() + charStr + formatInt(it.f_aram_kills3.toInt(), 3) + charStr + formatInt(it.f_aram_kills4.toInt(), 3) + charStr + formatInt(it.f_aram_kills5.toInt(), 2) })

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

    val query = QueryDsl
        .from(tbl_participantsnew)
        .leftJoin(tbl_matches) { tbl_matches.id eq tbl_participantsnew.match_id }
        .innerJoin(KORDLOLs.tbl_kordlols) { KORDLOLs.tbl_kordlols.LOL_id eq tbl_participantsnew.LOLperson_id }
        .where { tbl_matches.matchMode eq "ARAM" ; tbl_matches.bots eq false ; tbl_matches.surrender eq false ; tbl_matches.aborted eq false ; tbl_participantsnew.needCalcStats eq true }
        .selectAsEntity(tbl_participantsnew)

    val statClass = Toppartisipants()
    R2DBC.runQuery { query }.forEach {
        statClass.calculateField(it, "Убийств", it.kills.toDouble())
        statClass.calculateField(it, "Смертей", it.deaths.toDouble())
        statClass.calculateField(it, "Ассистов", it.assists.toDouble())
        statClass.calculateField(it, "KDA", it.kda)
        statClass.calculateField(it, "Пентакиллов", it.kills5.toDouble())
        statClass.calculateField(it, "Квадракиллов", it.kills4.toDouble())
        statClass.calculateField(it, "Трипплкиллов", it.kills3.toDouble())
        statClass.calculateField(it, "Даблкиллов", it.kills2.toDouble())
        statClass.calculateField(it, "Урон в минуту", it.damagePerMinute)
        statClass.calculateField(it, "Золота в минуту", it.goldPerMinute)
        statClass.calculateField(it, "Урона строениям", it.damageDealtToBuildings.toDouble())
        statClass.calculateField(it, "Урона поглощено", it.damageSelfMitigated.toDouble())
        statClass.calculateField(it, "Провёл в контроле(сек)", it.timeCCingOthers.toDouble())
        statClass.calculateField(it, "Наложил контроля(сек)", it.totalTimeCCDealt.toDouble())
        statClass.calculateField(it, "Обездвиживаний нанесено(сек)", it.enemyChampionImmobilizations.toDouble())
        statClass.calculateField(it, "Получено золота", it.goldEarned.toDouble())
        statClass.calculateField(it, "Критический удар", it.largestCriticalStrike.toDouble())
        statClass.calculateField(it, "Магического урона чемпионам", it.magicDamageDealtToChampions.toDouble())
        statClass.calculateField(it, "Физического урона чемпионам", it.physicalDamageDealtToChampions.toDouble())
        statClass.calculateField(it, "Чистого урона чемпионам", it.trueDamageDealtToChampions.toDouble())
        statClass.calculateField(it, "Убито миньонов", it.totalMinionsKilled.toDouble())
        statClass.calculateField(it, "Попаданий снежками", it.snowballsHit.toDouble())
        statClass.calculateField(it, "Наложено щитов союзникам", it.totalDamageShieldedOnTeammates.toDouble())
        statClass.calculateField(it, "Получено урона", it.totalDamageTaken.toDouble())
        statClass.calculateField(it, "Нанесено урона чемпионам", it.totalDamageDealtToChampions.toDouble())
        statClass.calculateField(it, "Лечение союзников", it.totalHealsOnTeammates.toDouble())
    }

    var resultText = ""
    statClass.getResults(sqlData).forEach {
        resultText += "* $it\n"
    }
    builder.content = "**ТОП сервера по параметрам (за матч)**\nОбновлено: ${TimeStamp.now()}\n" + resultText
    resultText = ""

    sqlData.guildSQL.messageIdTopUpdated = System.currentTimeMillis()
    sqlData.guildSQL.update()
}

suspend fun editMessageTopLolContent(builder: UserMessageModifyBuilder){
    printLog("[editMessageTopLolContent] 0")
    if (!sqlData.isHaveLastARAM) return
    printLog("[editMessageTopLolContent] 1")
    val statClass = Toplols()
    sqlData.dataSavedLOL.get().forEach {
        statClass.calculateField(it, "Игр", it.f_aram_games, false)
        statClass.calculateField(it, "ВинРейт", (it.f_aram_wins / it.f_aram_games) * 100.0, true)
        statClass.calculateField(it, "Пентакиллов", it.f_aram_kills5, false)
        statClass.calculateField(it, "Убийств", it.f_aram_kills, false)
        statClass.calculateField(it, "Винстрик", it.countStreak(0).toDouble(), false)
        statClass.calculateField(it, "Лузстрик", it.countStreak(1).toDouble(), false)
        statClass.calculateField(it, "Оценок S", it.countGrade(0).toDouble(), false)
        statClass.calculateField(it, "Оценок A", it.countGrade(1).toDouble(), false)
        statClass.calculateField(it, "Оценок B", it.countGrade(2).toDouble(), false)
        statClass.calculateField(it, "Оценок C", it.countGrade(3).toDouble(), false)
        statClass.calculateField(it, "Оценок D", it.countGrade(4).toDouble(), false)
    }
    printLog("[editMessageTopLolContent] 2")

    var resultText = ""
    statClass.getTopAll().forEach { (s, topLolObjects) ->
        resultText += "$s\n"
        topLolObjects.forEach { lo ->
            resultText += "\t$lo\n"
        }
    }
    printLog("[editMessageTopLolContent] 3")
    builder.content = "**ТОП сервера по параметрам ARAM (всего)**\nОбновлено: ${TimeStamp.now()}\n" + resultText
    resultText = ""
    printLog("[editMessageTopLolContent] 4")
    statClass.getTopAll().clear()
    printLog("[editMessageTopLolContent] 5")
}

suspend fun editMessageMasteriesContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC) {
    if (sqlData.isNeedUpdateDays) sqlData.isNeedUpdateDays = false

    builder.content = "**Мастерство чемпионов (ТОП 3)**\nОбновлено: ${TimeStamp.now()}\n"
    val charStr = " / "

    val savedPartsHash = HashMap<LOLs, ArrayList<ChampionMasteryDtoItem>>()
    val kordlols = sqlData.dataKORDLOL.get()
    sqlData.dataSavedLOL.get().forEach { lol ->
        if (lol.LOL_puuid.isEmpty()) return@forEach
        if (kordlols.find { kl -> kl.LOL_id == lol.id } == null) return@forEach
        savedPartsHash[lol] = ArrayList()
        LeagueMainObject.catchChampionMasteries(lol.LOL_puuid, lol.LOL_region).forEach { dto ->
            savedPartsHash[lol]!!.add(dto)
        }
        while (savedPartsHash[lol]!!.size < 3) {
            savedPartsHash[lol]!!.add(ChampionMasteryDtoItem(0, 0, 0, 0, 0, false, 0, "", "", 0))
        }
    }

    val savedParts = savedPartsHash.map { it.key to it.value }.sortedBy { it.first.show_code }.toMap()

    val mainDataList1 = (savedParts.map { formatInt(it.key.show_code, 2) + "| " + R2DBC.getHeroFromKey(it.value[0].championId.toString())?.nameRU.toMaxSymbols(8, "..") + charStr + R2DBC.getHeroFromKey(it.value[1].championId.toString())?.nameRU.toMaxSymbols(8, "..") + charStr + R2DBC.getHeroFromKey(it.value[2].championId.toString())?.nameRU.toMaxSymbols(8, "..") })
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

    savedPartsHash.clear()
}

suspend fun editMessageMainDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC) {
    val sizeMatches = Matches().getSize()
    val sizeLOLs = LOLs().getSize()
    val sizeParticipants = ParticipantsNew().getSize()
    val sizeHeroes = R2DBC.stockHEROES.get().size

    var contentText = "**Статистика Главная**\nОбновлено: ${TimeStamp.now()}\n"
    contentText += "* Матчей: $sizeMatches\n"
    contentText += "* Игроков: $sizeLOLs\n"
    contentText += "* Данных (строк): ~${sizeParticipants + sizeMatches + sizeLOLs}\n"
    contentText += "* Чемпионов: $sizeHeroes\n"
    contentText += "* Версия игры: ${LeagueMainObject.LOL_VERSION}\n"
    builder.content = contentText

    if (!sqlData.isNeedUpdateDays && !sqlData.isHaveLastARAM) return

    val dataLols = sqlData.dataSavedLOL.get()
    dataLols.sortBy { it.show_code }

    val charStr = "/"

    val mainDataList1 = (dataLols.map { formatInt(it.show_code, 2) + charStr + sqlData.dataKORDLOL.get().find { dt -> dt.LOL_id == it.id }?.asUser(sqlData)?.lowDescriptor() })
    val mainDataList2 = (dataLols.map { it.getCorrectNameWithTag().toMaxSymbols(18, "..") + " " + charStr + " " + it.f_aram_winstreak })

    builder.embed {
        field {
            name = "ID/User"
            value = mainDataList1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Nickname/WinStreak"
            value = mainDataList2.joinToString(separator = "\n")
            inline = true
        }
    }
}

suspend fun editMessageAramMMRDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC) {

    printLog("[editMessageAramMMRDataContent] 1")
    builder.content = "**Статистика ММР**\nОбновлено: ${TimeStamp.now()}\n"
    printLog("[editMessageAramMMRDataContent] 2")
    if (!sqlData.isHaveLastARAM) return
    printLog("[editMessageAramMMRDataContent] 3")
    val charStr = " / "
    val aramData = sqlData.getArrayAramMMRData()
    printLog("[editMessageAramMMRDataContent] 4")

    aramData.sortBy { it.LOL?.show_code }
    printLog("[editMessageAramMMRDataContent] 5")

    val mainDataList1 = (aramData.map {
        val textBold = if (it.bold) "**" else ""
        textBold + formatInt(it.LOL?.show_code, 2) + "| " + PlayerRank.fromMMR(it.mmr_aram) + textBold
    })
    val mainDataList2 = (aramData.map {
        val textBold = if (it.bold) "**" else ""
        textBold + it.mmr_aram.toString() + textBold
    })
    val mainDataList3 = (aramData.map {
        val textBold = if (it.bold) "**" else ""
        textBold + R2DBC.getHeroFromKey(it.champion_id.toString())?.nameRU + charStr + it.mmr + " " + it.mvp_lvp_info + " " + it.LOL?.f_aram_last_key?.fromHexInt() + textBold
    })
    printLog("[editMessageAramMMRDataContent] 6")
    builder.embed {
        field {
            name = "ARAM Rank"
            value = mainDataList1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "MMR"
            value = mainDataList2.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "LastGame/MMR"
            value = mainDataList3.joinToString(separator = "\n")
            inline = true
        }
    }

    aramData.clear()
}