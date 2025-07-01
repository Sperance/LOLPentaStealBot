package ru.descend.bot.postgre.calculating

import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.to1Digits
import kotlin.math.pow

enum class PlayerRank(
    val threshold: Double,
    val winModifier: Double,
    val loseModifier: Double
) {
    CHALLENGER(3000.0, 0.5, 1.3),
    GRANDMASTER(2500.0, 0.6, 1.2),
    MASTER(2000.0, 0.7, 1.1),
    DIAMOND(1800.0, 0.8, 1.0),
    PLATINUM(1500.0, 0.9, 0.9),
    GOLD(1200.0, 1.0, 0.8),
    SILVER(900.0, 1.1, 0.7),
    BRONZE(600.0, 1.2, 0.6),
    IRON(300.0, 1.3, 0.5);

    companion object {
        fun fromMMR(mmr: Double): PlayerRank {
            return entries.firstOrNull { mmr >= it.threshold } ?: IRON
        }
    }
}

class Calc_MMRv3(match: Matches) {

    companion object {
        private const val GAME_DURATION_THRESHOLD = 22.0 //Средняя продолжительность матча
    }

    // Настройки системы MMR
    private val baseKFactor = 20.0
    private val performanceImpact = 0.25
    private var textAllResult = ""
    private val matchMinutes = (match.matchDuration / 60.0).to1Digits()
    private val timeFactor = (matchMinutes / GAME_DURATION_THRESHOLD).to1Digits()

    // Базовые веса параметров
    private val baseWeights = mapOf(
        "kda" to 1.0,
        "multiKills" to 1.0,
        "damagePerMinute" to 1.0,
        "damageSelfMitigated" to 1.0,
        "killParticipation" to 1.0,
        "totalHealsOnTeammates" to 1.0,
        "timeCCingOthers" to 1.0,
        "goldPerMinute" to 1.0,
        "turretTakedowns" to 1.0,
        "minionsKilled" to 1.0,
        "survivedLowHP" to 1.0,
        "skillAccuracy" to 1.0,
        "outnumberedFights" to 1.0,
        "saveAllyFromDeath" to 1.0,
        "bountyGold" to 1.0,
    )

    // Модификаторы весов для разных ролей
    private val roleModifiers = mapOf(
        "DAMAGE" to mapOf(
            "damagePerMinute" to 1.4,
            "timeCCingOthers" to 0.7,
            "minionsKilled" to 0.8,
            "survivedLowHP" to 1.4,
        ),
        "TANK" to mapOf(
            "damagePerMinute" to 0.8,
            "timeCCingOthers" to 1.4,
            "survivedLowHP" to 1.2,
            "damageSelfMitigated" to 1.2
        ),
        "SUPPORT" to mapOf(
            "kda" to 0.6,
            "damagePerMinute" to 0.6,
            "timeCCingOthers" to 1.4,
            "skillAccuracy" to 0.8,
            "minionsKilled" to 1.2
        ),
        "BROUSER" to mapOf(
            "kda" to 1.2,
            "damagePerMinute" to 1.2,
            "timeCCingOthers" to 1.8,
            "skillAccuracy" to 0.8,
            "minionsKilled" to 1.2
        ),
        "HYBRID" to mapOf(
            "damagePerMinute" to 1.1,
            "timeCCingOthers" to 1.1,
            "minionsKilled" to 1.2
        )
    )

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

    private fun getTopsFromParticipant(part: ParticipantsNew): Int {
        var countTop = 0
        if (part.top_damagePerMinute) countTop++
        if (part.top_damageMitigated) countTop++
        if (part.top_kda) countTop++
        if (part.top_cc) countTop++
        if (part.top_creeps) countTop++
        if (part.top_healTeammates) countTop++
        if (part.top_goldPerMinute) countTop++
        if (part.top_accuracy) countTop++
        return countTop
    }

    /**
     * Основной метод для расчета нового MMR после матча.
     *
     * @param player Статистика игрока
     * @param currentMMR Текущий MMR игрока
     * @param teamMMR MMR членов команды (включая игрока)
     * @param enemyTeamMMR MMR противников
     * @param matchResult Результат матча (true - победа)
     * @return AramMatchResult с полной информацией о расчете
     */
    fun calculateNewMMR(
        player: ParticipantsNew,
        currentMMR: Double,
        teamMMR: List<Double>,
        enemyTeamMMR: List<Double>,
        matchResult: Boolean,
        matchObj: Matches
    ): AramMatchResult {

        textAllResult = ""

        textAllResult += "[timeFactor]:{$timeFactor}; "

        val detectedRole = detectRoleByStats(player)

        // Получение весов с учетом роли
        val roleWeights = getWeightsForRole(detectedRole)

        val topStats = getTopsFromParticipant(player)
        textAllResult += "[topStats]:{$topStats}; "

        // Расчет Performance Score
        val performanceScore = (calculatePerformanceScore(player, roleWeights) + topStats * 0.3).to1Digits()

        val oldRank = determineRank(currentMMR)
        val rankModifier = if (matchResult) oldRank.winModifier else oldRank.loseModifier

        val (expectedWin, actualResult) = calculateWinProbability(teamMMR, enemyTeamMMR, matchResult)
        textAllResult += "[expectedWin, actualResult]:{$expectedWin, $actualResult}; "

        val mmrChange = (baseKFactor * (actualResult - expectedWin) * (1.0 + performanceImpact * (performanceScore - 1.0)) * rankModifier).to1Digits()
        val newMMR = (currentMMR + mmrChange).to1Digits()

        // Определение ранга и оценки
        val rank = determineRank(newMMR)
        val matchGrade = calculateMatchGrade(performanceScore)

        player.gameMatchKey += "[$matchGrade:$performanceScore]"

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
            textResult = textAllResult,
            gameLength = matchObj.matchDuration
        )
    }

    /**
     * Определяет роль по статистическим показателям.
     */
    private fun detectRoleByStats(player: ParticipantsNew): String {

        textAllResult += "[detectRoleByStats]; "
        val damageRatio = player.damagePerMinute.to1Digits() / timeFactor
        textAllResult += "[damageRatio]:{${damageRatio}}; "

        val healRatioSelf = player.totalHeal.toDouble().to1Digits() / timeFactor
        textAllResult += "[healRatioSelf]:{${healRatioSelf}}; "

        val healRatioTeam = player.totalHealsOnTeammates.toDouble().to1Digits() / timeFactor
        textAllResult += "[healRatioTeam]:{${healRatioTeam}}; "

        val healTotal = (healRatioSelf + healRatioTeam).to1Digits()

        val ccRatio = player.timeCCingOthers.toDouble().to1Digits() / timeFactor
        textAllResult += "[ccRatio]:{${ccRatio}}; "

        val damageMitidatedRatio = player.damageSelfMitigated.toDouble().to1Digits() / timeFactor
        textAllResult += "[damageMitidatedRatio]:{${damageMitidatedRatio}}; "

        val skillAccuracyRatio = (player.skillshotsHit.toDouble() - player.skillshotsDodged).toInt()
        textAllResult += "[skillAccuracyRatio]:{${skillAccuracyRatio}}; "

        return when {
            // Танк - много CC и выживаний
            ccRatio > 50 && damageMitidatedRatio > 10000 && skillAccuracyRatio < 50 -> "TANK"
            // Саппорт - много хила и CC
            damageRatio < 1100 && healTotal > 12000 && ccRatio > 30 && skillAccuracyRatio > 20 -> "SUPPORT"
            // Дамагер - высокий урон, мало хила
            damageRatio > 2000 && healRatioTeam < 5000 && damageMitidatedRatio < 10000 && skillAccuracyRatio > 1 -> "DAMAGE"
            // Дамагер - высокий урон, мало хила
            damageRatio > 2000 && damageMitidatedRatio > 15000 -> "BROUSER"
            // Бесполезный - мало всего
            damageRatio < 1000 && ccRatio < 10 && healTotal < 10000 -> "USUSLESS"
            // Гибрид - баланс урона и хила
            else -> "HYBRID"
        }
    }

    /**
     * Возвращает веса параметров с учетом роли.
     */
    private fun getWeightsForRole(role: String): Map<String, Double> {
        return baseWeights.mapValues { (key, baseWeight) ->
            baseWeight * (roleModifiers[role]?.get(key) ?: 1.0)
        }
    }

    /**
     * Рассчитывает Performance Score (0.5-1.5) на основе статистики и весов.
     */
    private fun calculatePerformanceScore(player: ParticipantsNew, weights: Map<String, Double>): Double {
        val kda = normalizeParameter("kda", player.kda, 6.0)
        val multiKills = normalizeParameter("multiKills", player.kills4 * 0.4 + player.kills5, 1.0)
        val damagePerMinute = normalizeParameter("damagePerMinute", player.damagePerMinute, 2000 * timeFactor)
        val damageSelfMitigated = normalizeParameter("damageSelfMitigated", player.damageSelfMitigated, 60000 * timeFactor)
        val killParticipation = normalizeParameter("killParticipation", player.killParticipation, 1.0)
        val totalHealsOnTeammates = normalizeParameter("totalHealsOnTeammates", player.totalHealsOnTeammates, 15000 * timeFactor)
        val timeCCingOthers = normalizeParameter("timeCCingOthers", player.timeCCingOthers, 60 * timeFactor)
        val goldPerMinute = normalizeParameter("goldPerMinute", player.goldPerMinute, 1000 * timeFactor)
        val turretTakedowns = normalizeParameter("turretTakedowns", player.turretTakedowns, 2.0)
        val minionsKilled = normalizeParameter("minionsKilled", player.totalMinionsKilled, 30 * timeFactor)
        val survivedLowHP = normalizeParameter("survivedLowHP", player.survivedSingleDigitHpCount + player.tookLargeDamageSurvived, 2 * timeFactor)
        val skillAccuracy = normalizeParameter("skillAccuracy", player.skillshotsHit - player.skillshotsDodged, 30 * timeFactor)
        val outnumberedFights = normalizeParameter("outnumberedFights", player.outnumberedKills * 1.5 + player.soloKills * 0.3, 4 * timeFactor)
        val saveAllyFromDeath = normalizeParameter("saveAllyFromDeath", player.saveAllyFromDeath, 3 * timeFactor)
        val bountyGold = normalizeParameter("bountyGold", player.bountyGold, 400 * timeFactor)

        // Взвешенная сумма всех параметров
        return (kda * weights["kda"]!! +
                multiKills * weights["multiKills"]!! +
                damagePerMinute * weights["damagePerMinute"]!! +
                damageSelfMitigated * weights["damageSelfMitigated"]!! +
                killParticipation * weights["killParticipation"]!! +
                totalHealsOnTeammates * weights["totalHealsOnTeammates"]!! +
                timeCCingOthers * weights["timeCCingOthers"]!! +
                goldPerMinute * weights["goldPerMinute"]!! +
                turretTakedowns * weights["turretTakedowns"]!! +
                minionsKilled * weights["minionsKilled"]!! +
                survivedLowHP * weights["survivedLowHP"]!! +
                skillAccuracy * weights["skillAccuracy"]!! +
                outnumberedFights * weights["outnumberedFights"]!! +
                saveAllyFromDeath * weights["saveAllyFromDeath"]!! +
                bountyGold * weights["bountyGold"]!!
                )
    }

    private fun normalizeParameter(paramName: String, paramValue: Number, maxValue: Double): Double {
        val normalStat = normalize(paramValue.toDouble().to1Digits(), maxValue.to1Digits()).to1Digits()
        textAllResult += "[$paramName]:{normalized: $normalStat, stock: ${paramValue.toDouble().to1Digits()}, need:${maxValue.to1Digits()}}; "
        return normalStat
    }

    /**
     * Определяет ранг игрока по MMR.
     */
    private fun determineRank(mmr: Double): PlayerRank {
        return PlayerRank.fromMMR(mmr)
    }

    /**
     * Рассчитывает оценку за матч (S+, A, B и т.д.).
     */
    private fun calculateMatchGrade(performanceScore: Double): String {
        return when {
            performanceScore >= 9 -> "S+"
            performanceScore >= 8 -> "S"
            performanceScore >= 7 -> "S-"
            performanceScore >= 6 -> "A"
            performanceScore >= 5 -> "B"
            performanceScore >= 4 -> "C"
            else -> "D"
        }
    }

    /**
     * Рассчитывает вероятность победы и фактический результат.
     */
    private fun calculateWinProbability(
        teamMMR: List<Double>,
        enemyMMR: List<Double>,
        isWin: Boolean
    ): Pair<Double, Double> {
        val teamAvg = teamMMR.average()
        val enemyAvg = enemyMMR.average()
        val expectedWin = 1.0 / (1.0 + 10.0.pow((enemyAvg - teamAvg) / 400.0))
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
                "lolName=$lolName, " +
                "lolChampion=$lolChampion, " +
                "textResult='${textResult.split(";").joinToString("\t\n")}')"
    }
}