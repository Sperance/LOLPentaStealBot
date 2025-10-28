package ru.descend.bot.postgre.calculating

import ru.descend.bot.fromHexInt
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.to1Digits
import ru.descend.bot.toHexInt
import java.math.BigInteger
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

enum class PlayerRank(
    val threshold: Double,
    val winModifier: Double,
    val loseModifier: Double
) {
    CHALLENGER(2200.0, 0.7, 1.2),
    GRANDMASTER(2000.0, 0.8, 1.2),
    MASTER(1800.0, 0.8, 1.1),
    DIAMOND(1500.0, 0.9, 1.0),
    PLATINUM(1200.0, 1.0, 0.9),
    GOLD(900.0, 1.0, 0.9),
    SILVER(600.0, 1.1, 0.8),
    BRONZE(300.0, 1.1, 0.8),
    IRON(0.0, 1.2, 0.7);

    companion object {
        fun fromMMR(mmr: Double): PlayerRank {
            return entries.firstOrNull { mmr >= it.threshold } ?: IRON
        }
    }
}

data class BaseWeight(val name: String, var needValue: Double, var mod: Double)
class AllBaseWeight {
    private val arrayWeights = ArrayList<BaseWeight>()

    init {
        arrayWeights.add(BaseWeight("kda", 6.0, 1.0))
        arrayWeights.add(BaseWeight("multiKills", 1.0, 1.0))
        arrayWeights.add(BaseWeight("damagePerMinute",1200.0, 1.0))
        arrayWeights.add(BaseWeight("damageSelfMitigated", 30000.0, 1.0))
        arrayWeights.add(BaseWeight("killParticipation", 1.0, 1.0))
        arrayWeights.add(BaseWeight("totalHealsOnTeammates", 10000.0, 1.0))
        arrayWeights.add(BaseWeight("timeCCingOthers", 50.0,  1.0))
        arrayWeights.add(BaseWeight("goldPerMinute", 900.0, 1.0))
        arrayWeights.add(BaseWeight("minionsKilled", 30.0, 1.0))
        arrayWeights.add(BaseWeight("survivedLowHP", 2.0, 1.0))
        arrayWeights.add(BaseWeight("skillAccuracy", 30.0, 1.0))
        arrayWeights.add(BaseWeight("outnumberedFights", 4.0, 1.0))
        arrayWeights.add(BaseWeight("saveAllyFromDeath", 4.0, 1.0))
        arrayWeights.add(BaseWeight("bountyGold", 400.0, 1.0))
        arrayWeights.add(BaseWeight("teamDamagePercentage", 0.3, 1.0))
    }

    fun changeByRole(role: String) {
        when (role) {
            "DAMAGE" -> {
                changeStat("kda", modV = 1.2)
                changeStat("damagePerMinute", needV = 1500.0)
                changeStat("timeCCingOthers", needV = 20.0, modV = 1.4)
                changeStat("minionsKilled", needV = 60.0, modV = 0.8)
                changeStat("damageSelfMitigated", needV = 8000.0, modV = 0.8)
                changeStat("skillAccuracy", needV = 50.0)
                changeStat("teamDamagePercentage", needV = 0.3, modV = 1.2)
                changeStat("goldPerMinute", needV = 700.0)
            }
            "TANK" -> {
                changeStat("kda", needV = 4.0, modV = 1.1)
                changeStat("damagePerMinute", needV = 1100.0, modV = 1.6)
                changeStat("minionsKilled", needV = 15.0, modV = 1.2)
                changeStat("timeCCingOthers", needV = 70.0, modV = 1.4)
                changeStat("survivedLowHP", modV = 1.4)
                changeStat("damageSelfMitigated", needV = 80000.0, modV = 0.8)
                changeStat("skillAccuracy", needV = 1.0)
                changeStat("teamDamagePercentage", modV = 1.2)
            }
            "SUPPORT" -> {
                changeStat("kda", needV = 10.0, modV = 0.6)
                changeStat("damagePerMinute", needV = 1100.0, modV = 1.2)
                changeStat("timeCCingOthers", needV = 60.0, modV = 1.4)
                changeStat("skillAccuracy", needV = 30.0, modV = 0.8)
                changeStat("minionsKilled", modV = 1.2)
                changeStat("totalHealsOnTeammates", needV = 20000.0, modV = 1.2)
            }
            "BROUSER" -> {
                changeStat("kda", modV = 1.2)
                changeStat("damagePerMinute", needV = 2000.0, modV = 1.2)
                changeStat("timeCCingOthers", needV = 40.0, modV = 1.8)
                changeStat("skillAccuracy", needV = 1.0)
                changeStat("minionsKilled", needV = 40.0, modV = 1.2)
                changeStat("teamDamagePercentage", modV = 1.3)
            }
            "HYBRID" -> {
                changeStat("damagePerMinute", needV = 1300.0, modV = 1.2)
                changeStat("timeCCingOthers", needV = 30.0, modV = 1.2)
                changeStat("skillAccuracy", needV = 5.0)
                changeStat("bountyGold", needV = 300.0)
            }
            else -> Unit
        }
    }

    private fun getWeightByName(name: String): BaseWeight? {
        return arrayWeights.firstOrNull { it.name == name }
    }

    fun getModByName(name: String): Double {
        return getWeightByName(name)?.mod!!
    }

    fun getMaxByName(name: String): Double {
        return getWeightByName(name)?.needValue!!
    }

    private fun changeStat(name: String, needV: Double? = null, modV: Double? = null) {
        arrayWeights.firstOrNull { it.name == name }?.apply {
            if (needV != null) needValue = needV
            if (modV != null) mod = modV
        }
    }
}

class Calc_MMRv3(private val match: Matches) {

    // Настройки системы MMR
    private val baseKFactor = 15.0
    private val allBaseWeight = AllBaseWeight()
    private var textAllResult = ""
    private val matchMinutes = (match.matchDuration / 60.0).to1Digits()
    private val timeFactor = ultraSmoothExponential(matchMinutes)

    private fun ultraSmoothExponential(x: Double): Double {
        return when (x) {
            8.0 -> 0.6
            9.0 -> 0.7
            10.0 -> 0.85
            else -> min(0.85 * (1.06.pow(x - 10)), 2.8)
        }
    }

    fun calculateTOPstats(list: List<ParticipantsNew>) {
        list.maxByOrNull { it.damagePerMinute }!!.top_damagePerMinute = true
        list.maxByOrNull { it.damageSelfMitigated }!!.top_damageMitigated = true
        list.maxByOrNull { it.kda }!!.top_kda = true
        list.maxByOrNull { it.timeCCingOthers }!!.top_cc = true
        list.maxByOrNull { it.totalMinionsKilled }!!.top_creeps = true
        list.maxByOrNull { it.effectiveHealAndShielding }!!.top_healTeammates = true
        list.maxByOrNull { it.goldPerMinute }!!.top_goldPerMinute = true
        list.maxByOrNull { it.skillshotsHit.toDouble() - it.skillshotsDodged }!!.top_accuracy = true
    }

    private fun getTopsFromParticipant(part: ParticipantsNew): String {
        var countTop = ""
        if (part.top_damagePerMinute) countTop += "top_damagePerMinute;"
        if (part.top_damageMitigated) countTop += "top_damageMitigated;"
        if (part.top_kda) countTop += "top_kda;"
        if (part.top_cc) countTop += "top_cc;"
        if (part.top_creeps) countTop += "top_creeps;"
        if (part.top_healTeammates) countTop += "top_healTeammates;"
        if (part.top_goldPerMinute) countTop += "top_goldPerMinute;"
        if (part.top_accuracy) countTop += "top_accuracy;"
        return countTop
    }

    /**
     * Основной метод для расчета нового MMR после матча.
     *
     * @return AramMatchResult с полной информацией о расчете
     */
    fun calculateNewMMR(
        player: ParticipantsNew,
        lol: LOLs,
        matchResult: Boolean
    ): AramMatchResult {

        textAllResult = ";[timeFactor]:{$timeFactor}; "
        textAllResult += "[matchMinutes]:{$matchMinutes}; "
        textAllResult += "[matchId]:{${match.matchId}}; "

        val detectedRole = detectRoleByStats(player)

        // Получение весов с учетом роли
        allBaseWeight.changeByRole(detectedRole)

        val topStats = getTopsFromParticipant(player)
        textAllResult += "[topStats]:{$topStats, count:${topStats.count { it == ';' }}}; "

        // Расчет Performance Score
        var performanceScore = (calculatePerformanceScore(player, allBaseWeight) + topStats.count { it == ';' } * 0.0).to1Digits()

        val oldRank = PlayerRank.fromMMR(lol.mmrAram)
        val rankModifier = if (matchResult) oldRank.winModifier
        else oldRank.loseModifier

        performanceScore = performanceScore.to1Digits()

        val (expectedWin, actualResult) = calculateWinProbability(matchResult)
        textAllResult += "[expectedWin, actualResult]:{$expectedWin, $actualResult}; "

        val matchGrade = calculateMatchGrade(performanceScore)

        val mmrChange = if (player.win) {
            textAllResult += "[mmrChange:$baseKFactor * 1.5 * ($performanceScore / 10.0) * $rankModifier]; "
            ((baseKFactor * 1.5 * (performanceScore / 10.0)) * rankModifier).to1Digits()
        } else {
            textAllResult += "[mmrChange:$baseKFactor * -0.9 * (8.0 / $performanceScore) * $rankModifier]; "
            ((baseKFactor * -0.9 * (8.0 / performanceScore)) * rankModifier).to1Digits()
        }


        var newMMR = (lol.mmrAram + mmrChange).to1Digits()
        if (newMMR < 0.0) newMMR = 0.0

        // Определение ранга и оценки
        val rank = PlayerRank.fromMMR(newMMR)

        lol.mmrAram = newMMR
        player.gameMatchMmr = mmrChange
        lol.f_aram_last_key = "[$matchGrade:$detectedRole]".toHexInt()
        lol.f_aram_grades = calcGradeLOL(lol, matchGrade).toHexInt()
        lol.f_aram_roles = calcRolesLOL(lol, detectedRole).toHexInt()

        if (lol.f_aram_winstreak > lol.countStreak(0))
            lol.f_aram_streaks = "W:${lol.f_aram_winstreak};L:${lol.countStreak(1)};".toHexInt()

        if (lol.f_aram_winstreak < 0 && (abs(lol.f_aram_winstreak) > abs(lol.countStreak(1))))
            lol.f_aram_streaks = "W:${lol.countStreak(0)};L:${abs(lol.f_aram_winstreak)};".toHexInt()

        return AramMatchResult(
            newMMR = newMMR,
            mmrChange = mmrChange,
            performanceScore = performanceScore,
            matchGrade = matchGrade,
            rank = rank.name,
            adjustedStats = player,
            detectedRole = detectedRole,
            lolName = player.riotIdGameName,
            lolChampion = player.championName,
            match = match,
            textResult = textAllResult,
            topStatsCounter = topStats.count { it == ';' },
            lol = lol,
            gameLength = match.matchDuration
        )
    }

    private fun calcGradeLOL(lol: LOLs, matchGrade: String): String {
        if (lol.f_aram_grades == BigInteger.ZERO) {
            lol.f_aram_grades = "S:0;A:0;B:0;C:0;D:0;".toHexInt()
        }
        val countS = lol.countGrade(0) + matchGrade.count { it == 'S' }
        val countA = lol.countGrade(1) + matchGrade.count { it == 'A' }
        val countB = lol.countGrade(2) + matchGrade.count { it == 'B' }
        val countC = lol.countGrade(3) + matchGrade.count { it == 'C' }
        val countD = lol.countGrade(4) + matchGrade.count { it == 'D' }
        return "S:$countS;A:$countA;B:$countB;C:$countC;D:$countD;"
    }

    private fun calcRolesLOL(lol: LOLs, role: String): String {
        if (lol.f_aram_roles == BigInteger.ZERO) {
            lol.f_aram_roles = "D:0;B:0;T:0;S:0;U:0;H:0;".toHexInt()
        }
        val countD = lol.countRoles(0) + (if (role.firstOrNull() == 'D') 1 else 0)
        val countB = lol.countRoles(1) + (if (role.firstOrNull() == 'B') 1 else 0)
        val countT = lol.countRoles(2) + (if (role.firstOrNull() == 'T') 1 else 0)
        val countS = lol.countRoles(3) + (if (role.firstOrNull() == 'S') 1 else 0)
        val countU = lol.countRoles(4) + (if (role.firstOrNull() == 'U') 1 else 0)
        val countH = lol.countRoles(5) + (if (role.firstOrNull() == 'H') 1 else 0)
        return "D:$countD;B:$countB;T:$countT;S:$countS;U:$countU;H:$countH;"
    }

    /**
     * Определяет роль по статистическим показателям.
     */
    private fun detectRoleByStats(player: ParticipantsNew): String {

        val damageRatio = (player.damagePerMinute).to1Digits()
        textAllResult += "[damageRatio]:{${damageRatio}, stock: ${player.damagePerMinute.to1Digits()}}; "

        val healRatioSelf = (player.totalHeal.toDouble()).to1Digits()
        textAllResult += "[healRatioSelf]:{${healRatioSelf}, stock: ${player.totalHeal}}; "

        val healRatioTeam = (player.totalHealsOnTeammates.toDouble()).to1Digits()
        textAllResult += "[healRatioTeam]:{${healRatioTeam}, stock: ${player.totalHealsOnTeammates}}; "

        val healTotal = (healRatioSelf + healRatioTeam).to1Digits()

        val ccRatio = (player.timeCCingOthers.toDouble()).to1Digits()
        textAllResult += "[ccRatio]:{${ccRatio}, stock: ${player.timeCCingOthers}}; "

        val damageMitidatedRatio = (player.damageSelfMitigated.toDouble()).to1Digits()
        textAllResult += "[damageMitidatedRatio]:{${damageMitidatedRatio}, stock: ${player.damageSelfMitigated}}; "

        val skillAccuracyRatio = (player.skillshotsHit.toDouble() - player.skillshotsDodged).toInt()
        textAllResult += "[skillAccuracyRatio]:{${skillAccuracyRatio}}; "

        return when {
            damageRatio > 1200 && damageMitidatedRatio < 40000 -> "DAMAGE"
            damageRatio > 1200 && damageMitidatedRatio > 40000 -> "BROUSER"
            (damageMitidatedRatio > 30000 && ccRatio > 60) || damageMitidatedRatio > 60000 -> "TANK"
            damageRatio < 1000 && (healRatioTeam > 10000 || skillAccuracyRatio > 10) -> "SUPPORT"
            damageRatio < 1100 && ccRatio < 10 && healTotal < 10000 && damageMitidatedRatio < 10000 -> "USELESS"
            else -> "HYBRID"
        }
    }

    /**
     * Рассчитывает Performance Score (0.5-1.5) на основе статистики и весов.
     */
    private fun calculatePerformanceScore(player: ParticipantsNew, weights: AllBaseWeight): Double {
        val kda = normalizeParameter("kda", player.kda, false)
        val multiKills = normalizeParameter("multiKills", player.kills4 * 0.4 + player.kills5, false)
        val damagePerMinute = normalizeParameter("damagePerMinute", player.damagePerMinute, true)
        val damageSelfMitigated = normalizeParameter("damageSelfMitigated", player.damageSelfMitigated, true)
        val killParticipation = normalizeParameter("killParticipation", player.killParticipation, false)
        val totalHealsOnTeammates = normalizeParameter("totalHealsOnTeammates", player.totalHealsOnTeammates, true)
        val timeCCingOthers = normalizeParameter("timeCCingOthers", player.timeCCingOthers, true)
        val goldPerMinute = normalizeParameter("goldPerMinute", player.goldPerMinute, true)
        val minionsKilled = normalizeParameter("minionsKilled", player.totalMinionsKilled, true)
        val survivedLowHP = normalizeParameter("survivedLowHP", player.survivedSingleDigitHpCount + player.tookLargeDamageSurvived, true)
        val skillAccuracy = normalizeParameter("skillAccuracy", player.skillshotsHit - player.skillshotsDodged, true)
        val outnumberedFights = normalizeParameter("outnumberedFights", player.outnumberedKills * 1.5 + player.soloKills * 0.3, false)
        val saveAllyFromDeath = normalizeParameter("saveAllyFromDeath", player.saveAllyFromDeath, true)
        val bountyGold = normalizeParameter("bountyGold", player.bountyGold, false)
        val teamDamagePercentage = normalizeParameter("teamDamagePercentage", player.teamDamagePercentage, false)

        textAllResult += "[perfectGame]:{${player.perfectGame}}; "

        // Взвешенная сумма всех параметров
        return (kda * weights.getModByName("kda") +
                multiKills * weights.getModByName("multiKills") +
                damagePerMinute * weights.getModByName("damagePerMinute") +
                damageSelfMitigated * weights.getModByName("damageSelfMitigated") +
                killParticipation * weights.getModByName("killParticipation") +
                totalHealsOnTeammates * weights.getModByName("totalHealsOnTeammates") +
                timeCCingOthers * weights.getModByName("timeCCingOthers") +
                goldPerMinute * weights.getModByName("goldPerMinute") +
                minionsKilled * weights.getModByName("minionsKilled") +
                survivedLowHP * weights.getModByName("survivedLowHP") +
                skillAccuracy * weights.getModByName("skillAccuracy") +
                outnumberedFights * weights.getModByName("outnumberedFights") +
                saveAllyFromDeath * weights.getModByName("saveAllyFromDeath") +
                bountyGold * weights.getModByName("bountyGold") +
                teamDamagePercentage * weights.getModByName("teamDamagePercentage")
                )
    }

    private fun normalizeParameter(paramName: String, paramValue: Number, needTimeFactor: Boolean): Double {
        val factor = if (needTimeFactor) timeFactor else 1.0
        val normalStat = normalize(paramValue.toDouble().to1Digits(), allBaseWeight.getMaxByName(paramName) * factor).to1Digits()
        textAllResult += "[$paramName]:{normalized: $normalStat, stock: ${paramValue.toDouble().to1Digits()}, need:${(allBaseWeight.getMaxByName(paramName) * factor).to1Digits()}}; "
        return normalStat
    }

    /**
     * Рассчитывает оценку за матч (S+, A, B и т.д.).
     */
    private fun calculateMatchGrade(performanceScore: Double): String {
        return when {
            performanceScore >= 10 -> "S+"
            performanceScore >= 9 -> "S"
            performanceScore >= 8 -> "S-"
            performanceScore >= 7 -> "A+"
            performanceScore >= 6.5 -> "A"
            performanceScore >= 6 -> "A-"
            performanceScore >= 5.5 -> "B+"
            performanceScore >= 5 -> "B"
            performanceScore >= 4.5 -> "B-"
            performanceScore >= 4 -> "C+"
            performanceScore >= 3.5 -> "C"
            performanceScore >= 3 -> "C-"
            else -> "D"
        }
    }

    /**
     * Рассчитывает вероятность победы и фактический результат.
     */
    private fun calculateWinProbability(isWin: Boolean): Pair<Double, Double> {
        val expectedWin = 1.0 / (1.0 + 10.0.pow(1 / 400.0))
        val actualResult = if (isWin) 1.0 else 0.0
        return expectedWin to actualResult
    }

    /**
     * Нормализует значение в диапазон [0, 1].
     */
    private fun normalize(value: Double, max: Double): Double {
        return (value / max).coerceIn(0.0, 1.0).to1Digits()
    }
}

/**
 * Результат расчета MMR после матча.
 */
data class AramMatchResult(
    val newMMR: Double,                                 // Новый MMR после матча
    val mmrChange: Double,                              // Изменение MMR
    val performanceScore: Double,                       // Оценка эффективности (0.5-1.5)
    val matchGrade: String,                             // Оценка за матч (S+, A, B и т.д.)
    val rank: String,                                   // Текущий ранг
    val adjustedStats: ParticipantsNew,                 // Статистика с учетом времени игры
    val detectedRole: String,                           // Определенная роль
    val gameLength: Int,                                // Продолжительность игры
    val lolName: String,
    val lolChampion: String,
    val match: Matches,
    val topStatsCounter: Int,
    val lol: LOLs,
    val textResult: String,                             // Результат всех вычислений как текст
) {
    override fun toString(): String {
        return "AramMatchResult(" +
                "newMMR=$newMMR, " +
                "mmrChange=$mmrChange, " +
                "performanceScore=$performanceScore, " +
                "matchGrade='$matchGrade', " +
                "rank='$rank', " +
                "detectedRole='$detectedRole', " +
                "gameLength=$gameLength, " +
                "matchId=${match.matchId}, " +
                "lolName=$lolName, " +
                "lolChampion=$lolChampion, " +
                "textResult='${textResult.split(";").joinToString("\t\n")}')"
    }

    fun toStringLow(): String {
        return "\n-----\nMMR=$mmrChange" +
                "\n  Очков=$performanceScore" +
                "\n  Оценка='$matchGrade'" +
                "\n  Ранг='$rank'" +
                "\n  Роль='$detectedRole'" +
                "\n  ДлительностьИгры=$gameLength" +
                "\n  ID Игры=${match.matchId}" +
                "\n  ТОП статов=$topStatsCounter" +
                "\n  Оценки=${lol.f_aram_grades.fromHexInt()}" +
                "\n  Стрики=${lol.f_aram_streaks.fromHexInt()}" +
                "\n  Роли=${lol.f_aram_roles.fromHexInt()}" +
                "\n  Игрок=$lolName" +
                "\n  Чемпион=$lolChampion"
    }
}