package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.double
import org.komapper.core.dsl.query.int
import org.komapper.core.dsl.query.string
import ru.descend.bot.asyncLaunch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.lolapi.leaguedata.match_dto.Participant
import ru.descend.bot.lowDescriptor
import ru.descend.bot.mail.GMailSender
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.WorkData
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.model.tbl_KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_KORDs
import ru.descend.bot.postgre.r2dbc.model.tbl_LOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_matches
import ru.descend.bot.postgre.r2dbc.model.tbl_participants
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.CalculateMMR_2
import ru.descend.bot.savedObj.isCurrentDay
import ru.descend.bot.sendMessage
import ru.descend.bot.toDate
import ru.descend.bot.toFormatDateTime
import java.util.WeakHashMap

data class kordTemp_r2(var kordLOL: KORDLOLs, var lastParts: ArrayList<Participants>, var isBold: Boolean)
data class statMainTemp_r2(var kord_lol_id: Int, var games: Int, var win: Int, var kill: Int, var kill2: Int, var kill3: Int, var kill4: Int, var kill5: Int)
data class statAramDataTemp_r2(var kord_lol_id: Int, var mmr_aram: Double, var mmr_aram_saved: Double, var games: Int?, var champion_id: Int?, var mmr: Double?, var match_id: String?, var last_match_id: String?, var bold: Boolean)

class SQLData_R2DBC (var guild: Guild, var guildSQL: Guilds) {

    var listKordTemp = ArrayList<kordTemp_r2>()
    var listMainData = ArrayList<KORDLOLs>()

    var mainDataList1: ThreadLocal<java.util.ArrayList<Any?>> = ThreadLocal.withInitial{ ArrayList() }
    var mainDataList2: ThreadLocal<java.util.ArrayList<Any?>> = ThreadLocal.withInitial{ ArrayList() }
    var mainDataList3: ThreadLocal<java.util.ArrayList<Any?>> = ThreadLocal.withInitial{ ArrayList() }

    val dataGuild = WorkData<Guilds>()
    val dataKORDLOL = WorkData<KORDLOLs>()
    val dataKORD = WorkData<KORDs>()
    val dataLOL = WorkData<LOLs>()
    val dataMatches = WorkData<Matches>()
    val dataMMR = WorkData<MMRs>()
    val dataParticipants = WorkData<Participants>()

    val dataSavedLOL = WorkData<LOLs>()
    val dataSavedParticipants = WorkData<Participants>()

    fun initialize() {
        dataGuild.bodyReset = { R2DBC.getGuilds(null) }
        dataKORDLOL.bodyReset = { R2DBC.getKORDLOLs { tbl_KORDLOLs.guild_id eq guildSQL.id } }
        dataKORD.bodyReset = { R2DBC.getKORDs { tbl_KORDs.guild_id eq guildSQL.id } }
        dataLOL.bodyReset = { R2DBC.getLOLs(null) }
        dataMatches.bodyReset = { R2DBC.getMatches { tbl_matches.guild_id eq guildSQL.id } }
        dataMMR.bodyReset = { R2DBC.getMMRs(null) }
        dataParticipants.bodyReset = { R2DBC.getParticipants { tbl_participants.guild_id eq guildSQL.id } }

        dataSavedLOL.bodyReset = {
            val kordLol_lol_id = dataKORDLOL.get().map { it.LOL_id }
            R2DBC.db.withTransaction {
                R2DBC.db.runQuery {
                    QueryDsl.from(tbl_LOLs).where { tbl_LOLs.id.inList(kordLol_lol_id) }
                }
            }
        }

        dataSavedParticipants.bodyReset = {
            val kordLol_lol_id = dataKORDLOL.get().map { it.LOL_id }
            R2DBC.db.withTransaction {
                R2DBC.db.runQuery {
                    QueryDsl.from(tbl_participants).where { tbl_participants.LOLperson_id.inList(kordLol_lol_id) }
                }
            }
        }
    }

    fun performClear() {
        clearMainDataList()

        dataGuild.clear()
        dataKORDLOL.clear()
        dataKORD.clear()
        dataLOL.clear()
        dataMatches.clear()
        dataMMR.clear()
        dataParticipants.clear()

        dataSavedLOL.clear()
        dataSavedParticipants.clear()
    }

    fun clearMainDataList() {
        mainDataList1.get()?.clear()
        mainDataList2.get()?.clear()
        mainDataList3.get()?.clear()
    }

    /*-----*/
    suspend fun getKORDLOL(reset: Boolean = false) = dataKORDLOL.get(reset)
    suspend fun getKORDLOL(id: Int) = getKORDLOL().find { it.id == id }
    /*-----*/
    suspend fun getLOL(reset: Boolean = false) = dataLOL.get(reset)
    suspend fun getLOL(id: Int) = getLOL().find { it.id == id }
    /*-----*/
    suspend fun getKORD(reset: Boolean = false) = dataKORD.get(reset)
    suspend fun geKORD(id: Int) = getKORD().find { it.id == id }
    /*-----*/

    private var arraySavedParticipants = ArrayList<statMainTemp_r2>()
    suspend fun resetSavedParticipants() {
        arraySavedParticipants.clear()
        R2DBC.db.withTransaction {
            R2DBC.db.runQuery {
                QueryDsl.fromTemplate("SELECT * FROM get_player_stats_param(${guildSQL.id})").select {
                    val id = it.int("id")?:0
                    val games = it.int("games")?:0
                    val win = it.int("win")?:0
                    val kill = it.int("kill")?:0
                    val kill2 = it.int("kill2")?:0
                    val kill3 = it.int("kill3")?:0
                    val kill4 = it.int("kill4")?:0
                    val kill5 = it.int("kill5")?:0
                    arraySavedParticipants.add(statMainTemp_r2(id, games, win, kill, kill2, kill3, kill4, kill5))
                }
            }
        }
    }
    suspend fun getSavedParticipants() : ArrayList<statMainTemp_r2> {
        if (arraySavedParticipants.isEmpty()) { resetSavedParticipants() }
        return arraySavedParticipants
    }

    /*-----*/

    private var arrayAramMMRData = ArrayList<statAramDataTemp_r2>()
    suspend fun resetArrayAramMMRData() {
        arrayAramMMRData.clear()
        R2DBC.db.withTransaction {
            R2DBC.db.runQuery {
                QueryDsl.fromTemplate("SELECT * FROM get_aram_data_param(${guildSQL.id})").select { row ->
                    val id = row.int("id")
                    val mmr_aram = row.double("mmr_aram")
                    val mmr_aram_saved = row.double("mmr_aram_saved")
                    val games = row.int("games")
                    val champion_id = row.int("champion_id")
                    val mmr = row.double("mmr")
                    val match_id = row.string("match_id")
                    val last_match_id = row.string("last_match_id")
                    arrayAramMMRData.add(statAramDataTemp_r2(id!!, mmr_aram!!, mmr_aram_saved!!, games, champion_id, mmr, match_id, last_match_id, false))
                }
            }
        }
    }
    suspend fun getArrayAramMMRData() : ArrayList<statAramDataTemp_r2> {
        if (arrayAramMMRData.isEmpty()) { resetArrayAramMMRData() }
        return arrayAramMMRData
    }

    /*-----*/

    suspend fun addMatch(match: MatchDTO) {
        val newMatch = addMatch(this, match, getKORDLOL())
        calculatePentaSteal(match, newMatch)
    }

    private suspend fun calculatePentaSteal(match: MatchDTO, mch: Matches) {
        var isNeedCalculate = false
        match.info.participants.forEach {
            if (it.quadraKills - it.pentaKills > 0) {
                isNeedCalculate = true
                return@forEach
            }
        }
        if (isNeedCalculate) {
            val parts = getParticipantsForMatch(mch.id)
            LeagueMainObject.catchPentaSteal(match.metadata.matchId).forEach {pair ->
                val firstPart = parts.find { getLOL(it.LOLperson_id)?.LOL_puuid == pair.first }
                var secondPart = parts.find { getLOL(it.LOLperson_id)?.LOL_puuid == pair.second }

                if (firstPart == null) {
                    sendEmail("Error", "Participant with PUUID ${pair.first} not found in SQL. Match:${match.metadata.matchId}")
                    return
                }

                if (secondPart == null) {
                    sendEmail("Error", "Participant with PUUID ${pair.second} not found in SQL. Match:${match.metadata.matchId}")
                    return
                }

                if (firstPart == secondPart) {
                    sendEmail("Error", "Participants with PUUID ${pair.first} are Equals. Match:${match.metadata.matchId}")
                    return
                }

                if (getKORDLOL().find { kl -> kl.LOL_id == firstPart.LOLperson_id } == null && getKORDLOL().find { kl -> kl.LOL_id == secondPart.LOLperson_id } == null) {
                    sendEmail("PENTASTEAL (${match.metadata.matchId})", "ХЗ какой чел ${getLOL(firstPart.LOLperson_id)?.LOL_riotIdName} на ${firstPart.championName} состили Пенту у хз кого ${getLOL(secondPart.LOLperson_id)?.LOL_riotIdName} на ${secondPart.championName}\n\n${pair.third}")
                    return
                }

                sendEmail("PENTASTEAL (${match.metadata.matchId})", "Чел ${getLOL(firstPart.LOLperson_id)?.LOL_riotIdName} на ${firstPart.championName} состили Пенту у ${getLOL(secondPart.LOLperson_id)?.LOL_riotIdName} на ${secondPart.championName}\n\n${pair.third}")
            }
        }
    }

    private var mapWinStreak = WeakHashMap<Int, Int>()
    suspend fun getWinStreak() : WeakHashMap<Int, Int> {
        if (mapWinStreak.isEmpty()) { resetWinStreak() }
        return mapWinStreak
    }
    suspend fun resetWinStreak() {
        mapWinStreak.clear()
        R2DBC.db.withTransaction {
            R2DBC.db.runQuery {
                QueryDsl.fromTemplate("SELECT * FROM get_streak_results_param(${guildSQL.id})").select { row ->
                    val pers = row.int("PERS")
                    val res = row.int("RES")?:0
                    val ZN = row.string("ZN")?:""
                    when (ZN) {
                        "+" -> mapWinStreak[pers] = res
                        "-" -> mapWinStreak[pers] = -res
                        else -> Unit
                    }
                }
            }
        }
    }

    suspend fun getNewMatches(list: ArrayList<String>): ArrayList<String> {
        dataMatches.get().forEach { list.remove(it.matchId) }
        return list
    }

    suspend fun getSavedParticipantsForMatch(matchId: Int) = dataSavedParticipants.get().filter { it.match_id == matchId }
    suspend fun getParticipantsForMatch(matchId: Int) = dataParticipants.get().filter { it.match_id == matchId }

    suspend fun getLOLforPUUID(puuid: String) = getLOL().find { it.LOL_puuid == puuid }

    suspend fun getMMRforChampion(championName: String) = dataMMR.get().find { it.champion == championName }

    fun sendEmail(theme: String, message: String) {
        try {
            GMailSender("llps.sys.bot@gmail.com", "esjk bphc hsjh otcx")
                .sendMail(
                    "[${guildSQL.name}] $theme",
                    message,
                    "llps.sys.bot@gmail.com",
                    "kaltemeis@gmail.com"
                )
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun executeProcedure(body: String) {
        QueryDsl.executeScript(body)
    }

    suspend fun addMatch(sqlData: SQLData_R2DBC, match: MatchDTO, kordLol: List<KORDLOLs>? = null) : Matches {

        var isBots = false
        var isSurrender = false
        match.info.participants.forEach {
            if (it.summonerId == "BOT" || it.puuid == "BOT") {
                isBots = true
            }
            if (it.gameEndedInEarlySurrender || it.teamEarlySurrendered) {
                isSurrender = true
            }
        }

        val pMatch = Matches(
            matchId = match.metadata.matchId,
            matchDateStart = match.info.gameStartTimestamp,
            matchDateEnd = match.info.gameEndTimestamp,
            matchDuration = match.info.gameDuration,
            matchMode = match.info.gameMode,
            matchGameVersion = match.info.gameVersion,
            guild_id = guildSQL.id,
            bots = isBots,
            surrender = isSurrender
        ).save()
        printLog("[R2DBC] added match: ${pMatch.id} ${pMatch.matchId}")

        if (pMatch.id % 1000 == 0){
            asyncLaunch {
                sqlData.sendEmail("Sys", "execute method AVGs()")
                executeProcedure("call \"GetAVGs\"()")
            }
        }

        val arrayHeroName = ArrayList<Participant>()
        match.info.participants.forEach {part ->
            arrayHeroName.add(part)
        }

        match.info.participants.forEach {part ->
            var curLOL = sqlData.getLOLforPUUID(part.puuid)

            if (kordLol != null && curLOL != null && !isBots && !isSurrender){
                kordLol.find { it.LOL_id == curLOL?.id }?.let {
                    if (part.pentaKills > 0 || (part.quadraKills - part.pentaKills) > 0) {
                        asyncLaunch {
                            if (part.pentaKills > 0 && (match.info.gameCreation.toDate().isCurrentDay() || match.info.gameEndTimestamp.toDate().isCurrentDay())) {
                                val textPentas = if (part.pentaKills == 1) "" else "(${part.pentaKills})"
                                sqlData.guild.sendMessage(guildSQL.messageIdStatus, "Поздравляем!!!\n${it.asUser(sqlData.guild, sqlData).lowDescriptor()} cделал Пентакилл$textPentas за ${LeagueMainObject.findHeroForKey(part.championId.toString())} убив: ${arrayHeroName.filter { it.teamId != part.teamId }.joinToString { LeagueMainObject.findHeroForKey(it.championId.toString()) }}\nМатч: ${match.metadata.matchId} Дата: ${match.info.gameCreation.toFormatDateTime()}")
                            }
                        }
                    }
                }
            }

            //Создаем нового игрока в БД
            if (curLOL == null) {
                curLOL = LOLs(LOL_puuid = part.puuid,
                    LOL_summonerId = part.summonerId,
                    LOL_summonerName = part.summonerName,
                    LOL_riotIdName = part.riotIdGameName,
                    LOL_riotIdTagline = part.riotIdTagline).save()
            }

            //Вдруг что изменится в профиле игрока
            if (curLOL.LOL_summonerLevel != part.summonerLevel || curLOL.LOL_summonerName != part.summonerName || curLOL.LOL_riotIdTagline != part.riotIdTagline || curLOL.LOL_summonerId != part.summonerId || curLOL.LOL_riotIdName != part.riotIdGameName) {
                curLOL.LOL_summonerName = part.summonerName
                curLOL.LOL_riotIdTagline = part.riotIdTagline
                curLOL.LOL_summonerId = part.summonerId
                curLOL.LOL_riotIdName = part.riotIdGameName
                curLOL.LOL_summonerLevel = part.summonerLevel
                curLOL = curLOL.update()
            }

            Participants(part, pMatch, curLOL).save()
        }

        if (kordLol != null) {
            calculateMMR(pMatch, isSurrender, isBots, kordLol)
        }

        return pMatch
    }

    private suspend fun calculateMMR(pMatch: Matches, isSurrender: Boolean, isBots: Boolean, kordLol: List<KORDLOLs>) {
        var users = ""
        val myParts = getSavedParticipantsForMatch(pMatch.id)

        myParts.forEach {
            val data = CalculateMMR_2(this, it, pMatch, kordLol, getMMRforChampion(it.championName))
            data.init()
            users += getLOL(it.LOLperson_id)?.LOL_summonerName + " hero: ${it.championName} $data\n"
        }
        guild.sendMessage(guildSQL.messageIdDebug,
            "Добавлен матч: ${pMatch.matchId} ID: ${pMatch.id}\n" +
                    "${pMatch.matchDateStart.toFormatDateTime()} - ${pMatch.matchDateEnd.toFormatDateTime()}\n" +
                    "Mode: ${pMatch.matchMode} Surrender: $isSurrender Bots: $isBots\n" +
                    "Users: $users")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SQLData_R2DBC

        if (guild != other.guild) return false
        if (guildSQL != other.guildSQL) return false
        if (listKordTemp != other.listKordTemp) return false
        if (listMainData != other.listMainData) return false
        if (mainDataList1 != other.mainDataList1) return false
        if (mainDataList2 != other.mainDataList2) return false
        if (mainDataList3 != other.mainDataList3) return false
        if (dataGuild != other.dataGuild) return false
        if (dataKORDLOL != other.dataKORDLOL) return false
        if (dataKORD != other.dataKORD) return false
        if (dataLOL != other.dataLOL) return false
        if (dataMatches != other.dataMatches) return false
        if (dataMMR != other.dataMMR) return false
        if (dataParticipants != other.dataParticipants) return false
        if (dataSavedLOL != other.dataSavedLOL) return false
        if (dataSavedParticipants != other.dataSavedParticipants) return false
        if (arraySavedParticipants != other.arraySavedParticipants) return false
        if (arrayAramMMRData != other.arrayAramMMRData) return false
        if (mapWinStreak != other.mapWinStreak) return false

        return true
    }

    override fun hashCode(): Int {
        var result = guild.hashCode()
        result = 31 * result + guildSQL.hashCode()
        result = 31 * result + listKordTemp.hashCode()
        result = 31 * result + listMainData.hashCode()
        result = 31 * result + mainDataList1.hashCode()
        result = 31 * result + mainDataList2.hashCode()
        result = 31 * result + mainDataList3.hashCode()
        result = 31 * result + dataGuild.hashCode()
        result = 31 * result + dataKORDLOL.hashCode()
        result = 31 * result + dataKORD.hashCode()
        result = 31 * result + dataLOL.hashCode()
        result = 31 * result + dataMatches.hashCode()
        result = 31 * result + dataMMR.hashCode()
        result = 31 * result + dataParticipants.hashCode()
        result = 31 * result + dataSavedLOL.hashCode()
        result = 31 * result + dataSavedParticipants.hashCode()
        result = 31 * result + arraySavedParticipants.hashCode()
        result = 31 * result + arrayAramMMRData.hashCode()
        result = 31 * result + mapWinStreak.hashCode()
        return result
    }
}