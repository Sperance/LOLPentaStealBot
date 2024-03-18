package ru.descend.bot.postgre.r2dbc.model

import dev.kord.core.entity.Guild
import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.QueryDsl
import ru.descend.bot.asyncLaunch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.lolapi.leaguedata.match_dto.Participant
import ru.descend.bot.lowDescriptor
import ru.descend.bot.mail.GMailSender
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.execProcedure
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.isCurrentDay
import ru.descend.bot.sendMessage
import ru.descend.bot.toDate
import ru.descend.bot.toFormatDateTime
import java.time.LocalDateTime

@KomapperEntity
@KomapperTable("tbl_guilds")
data class Guilds(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var idGuild: String = "",
    var name: String = "",
    var description: String = "",
    var ownerId: String = "",

    var botChannelId: String = "",
    var messageId: String = "",
    var messageIdStatus: String = "",
    var messageIdMain: String = "",
    var messageIdDebug: String = "",
    var messageIdPentaData: String = "",
    var messageIdGlobalStatisticData: String = "",
    var messageIdMasteryData: String = "",
    var messageIdArammmr: String = "",

    @KomapperCreatedAt
    val createdAt: LocalDateTime = LocalDateTime.MIN,
    @KomapperUpdatedAt
    val updatedAt: LocalDateTime = LocalDateTime.MIN
) {

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

        val pMatch = Matches.addMatch(Matches(
            matchId = match.metadata.matchId,
            matchDateStart = match.info.gameStartTimestamp,
            matchDateEnd = match.info.gameEndTimestamp,
            matchDuration = match.info.gameDuration,
            matchMode = match.info.gameMode,
            matchGameVersion = match.info.gameVersion,
            guild_id = this.id,
            bots = isBots,
            surrender = isSurrender
        ))!!
        printLog("[R2DBC] added match: ${pMatch.id} ${pMatch.matchId}")

        if (pMatch.id % 1000 == 0){
            asyncLaunch {
                sendEmail("Sys", "execute method AVGs()")
                execProcedure("call \"GetAVGs\"()")
            }
        }

        val arrayHeroName = ArrayList<Participant>()
        match.info.participants.forEach {part ->
            arrayHeroName.add(part)
        }

        match.info.participants.forEach {part ->
            var curLOL = R2DBC.getLOLforPUUID(part.puuid)

            if (kordLol != null && curLOL != null && !isBots && !isSurrender){
                kordLol.find { it.LOL_id == curLOL?.id }?.let {
                    if (part.pentaKills > 0 || (part.quadraKills - part.pentaKills) > 0) {
                        asyncLaunch {
                            if (part.pentaKills > 0 && (match.info.gameCreation.toDate().isCurrentDay() || match.info.gameEndTimestamp.toDate().isCurrentDay())) {
                                val textPentas = if (part.pentaKills == 1) "" else "(${part.pentaKills})"
                                sqlData.guild.sendMessage(messageIdStatus, "Поздравляем!!!\n${it.asUser(sqlData.guild, sqlData).lowDescriptor()} cделал Пентакилл$textPentas за ${LeagueMainObject.findHeroForKey(part.championId.toString())} убив: ${arrayHeroName.filter { it.teamId != part.teamId }.joinToString { LeagueMainObject.findHeroForKey(it.championId.toString()) }}\nМатч: ${match.metadata.matchId} Дата: ${match.info.gameCreation.toFormatDateTime()}")
                            }
                        }
                    }
                }
            }

            //Создаем нового игрока в БД
            if (curLOL == null) {
                curLOL = LOLs.addLOL(LOLs(
                    LOL_puuid = part.puuid,
                    LOL_summonerId = part.summonerId,
                    LOL_summonerName = part.summonerName,
                    LOL_riotIdName = part.riotIdGameName,
                    LOL_riotIdTagline = part.riotIdTagline)
                )
            }

            //Вдруг что изменится в профиле игрока
            if (curLOL != null) {
                if (curLOL.LOL_summonerLevel != part.summonerLevel || curLOL.LOL_summonerName != part.summonerName || curLOL.LOL_riotIdTagline != part.riotIdTagline || curLOL.LOL_summonerId != part.summonerId || curLOL.LOL_riotIdName != part.riotIdGameName) {
                    val textData = "[addMatch::update] LOL_summonerName: old ${curLOL.LOL_summonerName} new ${part.summonerName}" +
                            " LOL_riotIdTagline: old ${curLOL.LOL_riotIdTagline} new ${part.riotIdTagline}" +
                            " LOL_summonerId: old ${curLOL.LOL_summonerId} new ${part.summonerId}" +
                            " LOL_riotIdName: old ${curLOL.LOL_riotIdName} new ${part.riotIdGameName}" +
                            " LOL_summonerLevel: old: ${curLOL.LOL_summonerLevel} new ${part.summonerLevel}"
                    printLog(sqlData.guild, textData)

                    //если чтото меняется у сохраненных пользователей - отсылаем Email
                    if (kordLol != null && kordLol.find { it.LOL_id == curLOL.id } != null) {
                        sqlData.guildSQL.sendEmail("Update data", textData)
                    }

//                    curLOL.update(TableLOLPerson::LOL_summonerName, TableLOLPerson::LOL_riotIdTagline, TableLOLPerson::LOL_summonerId, TableLOLPerson::LOL_riotIdName, TableLOLPerson::LOL_summonerLevel){
//                        LOL_summonerName = part.summonerName
//                        LOL_summonerId = part.summonerId
//                        LOL_riotIdTagline = part.riotIdTagline
//                        LOL_riotIdName = part.riotIdGameName
//                        LOL_summonerLevel = part.summonerLevel
//                    }
                }
            }

            Participants.addParticipant(Participants(part, pMatch, curLOL!!))
        }

//        if (kordLol != null) {
//            calculateMMR(sqlData, pMatch, isSurrender, isBots, kordLol)
//        }

        return pMatch
    }

    fun sendEmail(theme: String, message: String) {
        try {
            GMailSender("llps.sys.bot@gmail.com", "esjk bphc hsjh otcx")
                .sendMail(
                    "[$name] $theme",
                    message,
                    "llps.sys.bot@gmail.com",
                    "kaltemeis@gmail.com"
                )
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        suspend fun addGuild(value: Guild) : Guilds? {
            var curGuild = Guilds()
            curGuild.idGuild = value.id.value.toString()
            curGuild.name = value.name
            curGuild.description = value.description ?: ""
            curGuild.ownerId = value.ownerId.value.toString()

            var result: Guilds? = null
            R2DBC.db.withTransaction {
                result = R2DBC.db.runQuery {
                    QueryDsl.insert(R2DBC.tbl_guilds).single(curGuild)
                }
                printLog("[R2DBC::addParticipant] added guild id ${result?.id} with idGuild ${result?.idGuild}")
            }
            return result
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Guilds

        if (id != other.id) return false
        if (idGuild != other.idGuild) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (ownerId != other.ownerId) return false
        if (botChannelId != other.botChannelId) return false
        if (messageId != other.messageId) return false
        if (messageIdStatus != other.messageIdStatus) return false
        if (messageIdMain != other.messageIdMain) return false
        if (messageIdDebug != other.messageIdDebug) return false
        if (messageIdPentaData != other.messageIdPentaData) return false
        if (messageIdGlobalStatisticData != other.messageIdGlobalStatisticData) return false
        if (messageIdMasteryData != other.messageIdMasteryData) return false
        if (messageIdArammmr != other.messageIdArammmr) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + idGuild.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + ownerId.hashCode()
        result = 31 * result + botChannelId.hashCode()
        result = 31 * result + messageId.hashCode()
        result = 31 * result + messageIdStatus.hashCode()
        result = 31 * result + messageIdMain.hashCode()
        result = 31 * result + messageIdDebug.hashCode()
        result = 31 * result + messageIdPentaData.hashCode()
        result = 31 * result + messageIdGlobalStatisticData.hashCode()
        result = 31 * result + messageIdMasteryData.hashCode()
        result = 31 * result + messageIdArammmr.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}