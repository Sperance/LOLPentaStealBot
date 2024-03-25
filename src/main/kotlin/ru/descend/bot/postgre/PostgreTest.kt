package ru.descend.bot.postgre

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.options.InsertOptions
import org.komapper.core.dsl.query.boolean
import org.komapper.core.dsl.query.double
import org.komapper.core.dsl.query.int
import org.komapper.core.dsl.query.long
import org.komapper.core.dsl.query.string
import ru.descend.bot.catchToken
import ru.descend.bot.lolapi.LeagueApi
import ru.descend.bot.lolapi.dataclasses.SavedPartSteal
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.model.tbl_KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_KORDs
import ru.descend.bot.postgre.r2dbc.model.tbl_LOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_MMRs
import ru.descend.bot.postgre.r2dbc.model.tbl_matches
import ru.descend.bot.postgre.r2dbc.model.tbl_participants
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.EnumMMRRank
import ru.descend.bot.savedObj.getStrongDate
import ru.descend.bot.toDate
import java.time.Duration

class PostgreTest {

    @Test
    fun test_mmrs(){
        printLog("MMR: ${EnumMMRRank.getMMRRank(42.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(420.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(0.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(89.5)}")
    }

    @Test
    fun testMethod() {
        printLog(1)
        val listIds = ArrayList<String>()
        listIds.add("RU_476092238")
        listIds.add("RE_476370823")
        listIds.add("RU_476367408")
    }

    @Test
    fun testCatchData_kordlols() {
        runBlocking {
            R2DBC.initialize()
            val kordList = ArrayList<KORDLOLs>()
            R2DBC.runQuery {
                QueryDsl.fromTemplate("select * from table_k_o_r_d__l_o_ls").select {
                    val data = KORDLOLs(
                        KORD_id = it.int("k_o_r_dperson")!!,
                        LOL_id = it.int("l_o_lperson")!!,
                        oldID = it.int("id")!!,
                        guild_id = it.int("guild")!!,
                        mmrAram = it.double("mmr_aram")!!,
                        mmrAramSaved = it.double("mmr_aram_saved")!!
                    )
                    printLog("added")
                    kordList.add(data)
                }
            }
            printLog("esa")
            kordList.forEach {
                printLog("it: ${it.id}")
                val kordid = it.KORD_id
                it.KORD_id = R2DBC.getKORDs { tbl_KORDs.oldID eq kordid }.first().id

                val lolid = it.LOL_id
                it.LOL_id = R2DBC.getLOLs { tbl_LOLs.oldID eq lolid }.first().id

                it.save()
            }
        }
    }

    @Test
    fun test_mmr() {
        runBlocking {
            printLog(R2DBC.getMMRs { tbl_MMRs.id eq 4 })
        }
    }

//    @Test
//    fun testCatchData_kords() {
//        runBlocking {
//            R2DBC.initialize()
//            val db = R2DBC.db
//            val kordList = ArrayList<KORDs>()
//            db.withTransaction {
//                db.runQuery {
//                    QueryDsl.fromTemplate("select * from table_k_o_r_d_persons").select {
//                        val k_o_r_d_id = it.string("k_o_r_d_id")!!
//                        val k_o_r_d_discriminator = it.string("k_o_r_d_discriminator")!!
//                        val k_o_r_d_name = it.string("k_o_r_d_name")!!
//                        val guild = it.int("guild")!!
//                        val id = it.int("id")!!
//                        val data = KORDs(
//                            oldID = id,
//                            KORD_id = k_o_r_d_id,
//                            KORD_name = k_o_r_d_name,
//                            KORD_discriminator = k_o_r_d_discriminator,
//                            guild_id = guild
//                        )
//                        kordList.add(data)
//                    }
//                }
//                kordList.forEach {
//                    it.save()
//                }
//            }
//        }
//    }

    @Test
    fun testCatchData_matches() {
        runBlocking {
            R2DBC.initialize()
            val kordList = ArrayList<Matches>()
            R2DBC.runQuery {
                QueryDsl.fromTemplate("select * from table_matches").select {
                    val data = Matches(
                        guild_id = it.int("guild")!!,
                        oldId = it.int("id")!!,
                        matchId = it.string("match_id")!!,
                        matchDateStart = it.long("match_date")!!,
                        matchDateEnd = it.long("match_date_end")!!,
                        matchDuration = it.int("match_duration")!!,
                        matchMode = it.string("match_mode")!!,
                        matchGameVersion = it.string("match_game_version")!!,
                        bots = it.boolean("bots")!!,
                        surrender = it.boolean("surrender")!!
                    )
                    kordList.add(data)
                }
            }
            kordList.forEach {
                it.save()
            }
        }
    }

    @Test
    fun testCatchData_lols() {
        runBlocking {
            R2DBC.initialize()
            val kordList = ArrayList<LOLs>()
            R2DBC.runQuery {
                QueryDsl.fromTemplate("select * from table_l_o_l_persons").select {
                    val l_o_l_puuid = it.string("l_o_l_puuid")!!
                    val l_o_l_region = it.string("l_o_l_region")!!
                    val l_o_l_riot_id_name = it.string("l_o_l_riot_id_name")?:""
                    val l_o_l_riot_id_tagline = it.string("l_o_l_riot_id_tagline")?:""
                    val l_o_l_summoner_id = it.string("l_o_l_summoner_id")!!?:""
                    val l_o_l_summoner_name = it.string("l_o_l_summoner_name")!!?:""
                    val l_o_l_account_id = it.string("l_o_l_account_id")!!
                    val l_o_l_summoner_level = it.int("l_o_l_summoner_level")?:0
                    val id = it.int("id")!!
                    val data = LOLs(
                        oldID = id,
                        LOL_puuid = l_o_l_puuid,
                        LOL_region = l_o_l_region,
                        LOL_riotIdName = l_o_l_riot_id_name,
                        LOL_riotIdTagline = l_o_l_riot_id_tagline,
                        LOL_summonerId = l_o_l_summoner_id,
                        LOL_summonerName = l_o_l_summoner_name,
                        LOL_accountId = l_o_l_account_id,
                        LOL_summonerLevel = l_o_l_summoner_level
                    )
                    kordList.add(data)
                }
            }
            kordList.sortBy { it.oldID }
            R2DBC.addBatchLOLs(kordList)
        }
    }

    @Test
    fun testCatchData_participants() {
        runBlocking {
            R2DBC.initialize()
            val kordList = ArrayList<Participants>()
            R2DBC.runQuery {
                QueryDsl.fromTemplate("select * from table_participants").select {
                    val data = Participants(
                        match_id = it.int("match")!!,
                        LOLperson_id = it.int("l_o_lperson")!!,
                        guild_id = 1,
                        championId = it.int("champion_id")!!,
                        championName = it.string("champion_name")!!,
                        kills5 = it.int("kills5")!!,
                        kills4 = it.int("kills4")!!,
                        kills3 = it.int("kills3")!!,
                        kills2 = it.int("kills2")!!,
                        kills = it.int("kills")!!,
                        assists = it.int("assists")!!,
                        deaths = it.int("deaths")!!,
                        goldEarned = it.int("gold_earned")!!,
                        skillsCast = it.int("skills_cast")!!,
                        totalDmgToChampions = it.int("total_dmg_to_champions")!!,
                        totalDamageShieldedOnTeammates = it.int("total_damage_shielded_on_teammates")!!,
                        totalHealsOnTeammates = it.int("total_heals_on_teammates")!!,
                        totalDamageTaken = it.int("total_damage_taken")!!,
                        damageDealtToBuildings = it.int("damage_dealt_to_buildings")!!,
                        timeCCingOthers = it.int("time_c_cing_others")!!,
                        skillshotsDodged = it.int("skillshots_dodged")!!,
                        enemyChampionImmobilizations = it.int("enemy_champion_immobilizations")!!,
                        damageTakenOnTeamPercentage = it.double("damage_taken_on_team_percentage")!!,
                        teamDamagePercentage = it.double("team_damage_percentage")!!,
                        damagePerMinute = it.double("damage_per_minute")!!,
                        kda = it.double("kda")!!,
                        mmr = it.double("mmr")!!,
                        mmrFlat = it.double("mmr_flat")!!,
                        minionsKills = it.int("minions_kills")!!,
                        inhibitorKills = it.int("inhibitor_kills")!!,
                        team = it.int("team")!!,
                        profileIcon = it.int("profile_icon")!!,
                        win = it.boolean("win")!!,
                        snowballsHit = it.int("snowballs_hit")!!,
                        skillshotsHit = it.int("skillshots_hit")!!,
                        soloKills = it.int("solo_kills")!!,
                        survivedSingleDigitHpCount = it.int("survived_single_digit_hp_count")!!,
                        magicDamageDealtToChampions = it.int("magic_damage_dealt_to_champions")!!,
                        physicalDamageDealtToChampions = it.int("physical_damage_dealt_to_champions")!!,
                        trueDamageDealtToChampions = it.int("true_damage_dealt_to_champions")!!,
                        effectiveHealAndShielding = it.double("effective_heal_and_shielding")!!,
                        damageSelfMitigated = it.int("damage_self_mitigated")!!,
                        largestCriticalStrike = it.int("largest_critical_strike")!!,
                        survivedThreeImmobilizesInFight = it.int("survived_three_immobilizes_in_fight")!!,
                        totalTimeCCDealt = it.int("total_time_c_c_dealt")!!,
                        tookLargeDamageSurvived = it.int("took_large_damage_survived")!!,
                        longestTimeSpentLiving = it.int("longest_time_spent_living")!!,
                        totalTimeSpentDead = it.int("total_time_spent_dead")!!
                    )
                    kordList.add(data)
                }
            }
            kordList.sortBy { it.match_id }
            val loles = R2DBC.getLOLs(null)
            val matches = R2DBC.getMatches(null)
            kordList.forEach {
                it.LOLperson_id = loles.find { lol -> lol.oldID == it.LOLperson_id }?.id?:-1
                it.match_id = matches.find { mch -> mch.oldId == it.match_id }?.id?:-1
            }
            printLog("start saving...")
            R2DBC.addBatchParticipants(kordList)
        }
    }

    @Test
    fun testtimeline() {
        val leagueApi = LeagueApi(catchToken()[1], LeagueApi.RU)
        val service = leagueApi.leagueService

        runBlocking {
            val exec = service.getMatchTimeline("RU_481300486")
            exec.body()?.let {
                val mapPUUID = HashMap<Long, String>()
                it.info.participants.forEach { part ->
                    mapPUUID[part.participantId] = part.puuid
                }

                var lastDate = System.currentTimeMillis()
                var removedPart: SavedPartSteal? = null
                var isNextCheck = false
                val arrayQuadras = ArrayList<SavedPartSteal>()

                it.info.frames.forEach { frame ->
                    frame.events.forEach lets@ { event ->
                        if (event.killerId != null && event.type.contains("CHAMPION")) {
                            val betw = Duration.between(lastDate.toDate().toInstant(), event.timestamp.toDate().toInstant())
                            val resDate = getStrongDate(event.timestamp)
                            lastDate = event.timestamp

                            printLog("EVENT: team:${if (event.killerId <= 5) "BLUE" else "RED"} killerId:${event.killerId} multiKillLength:${event.multiKillLength ?: 0} killType: ${event.killType?:""} type:${event.type} ${resDate.timeSec} STAMP: ${event.timestamp} BETsec: ${betw.toSeconds()}")

                            if (isNextCheck && (event.type == "CHAMPION_KILL" || event.type == "CHAMPION_SPECIAL_KILL")) {
                                arrayQuadras.forEach saved@ { sPart ->
                                    if (sPart.team == (if (event.killerId <= 5) "BLUE" else "RED") &&
                                        sPart.participantId != event.killerId) {
                                        printLog("PENTESTEAL. Чел PUUID ${mapPUUID[event.killerId]} состилил Пенту у ${sPart.puuid}")
                                        removedPart = sPart
                                        return@saved
                                    }
                                }
                                if (removedPart != null) {
                                    arrayQuadras.remove(removedPart)
                                    removedPart = null
                                }
                                isNextCheck = false
                            }
                            if (event.multiKillLength == 4L) {
                                arrayQuadras.add(SavedPartSteal(event.killerId, mapPUUID[event.killerId]?:"", if (event.killerId <= 5) "BLUE" else "RED", event.timestamp))
                                isNextCheck = true
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun checkMatchContains() {
        EnumMMRRank.entries.forEach {
            printLog("${it.nameRank} - ${it.ordinal} - ${((it.ordinal / 10.0) * 2.0) + 1.0}")
        }
    }
}