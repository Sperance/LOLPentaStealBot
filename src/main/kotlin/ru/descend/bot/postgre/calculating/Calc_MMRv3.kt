package ru.descend.bot.postgre.calculating

import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.to1Digits
import kotlin.math.pow

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
        "damagePerMinute" to 1.0,
        "killParticipation" to 1.0,
        "healTeam" to 1.0,
        "crowdControlScore" to 1.0,
        "objectiveParticipation" to 1.0,
        "goldPerMinute" to 1.0,
        "survivedLowHP" to 1.0,
        "skillAccuracy" to 1.0,
        "outnumberedFights" to 1.0,
        "killQueue" to 1.0,
        "DmgMitigated" to 1.0
    )

    // Модификаторы весов для разных ролей
    private val roleModifiers = mapOf(
        "DAMAGE" to mapOf(
            "damagePerMinute" to 1.5,
            "crowdControlScore" to 0.7
        ),
        "TANK" to mapOf(
            "damagePerMinute" to 0.8,
            "crowdControlScore" to 1.5,
            "survivedLowHP" to 1.3,
            "DmgMitigated" to 1.2
        ),
        "SUPPORT" to mapOf(
            "kda" to 0.6,
            "damagePerMinute" to 0.6,
            "crowdControlScore" to 1.4,
            "skillAccuracy" to 0.8
        ),
        "BROUSER" to mapOf(
            "kda" to 1.2,
            "damagePerMinute" to 1.2,
            "crowdControlScore" to 1.8,
            "skillAccuracy" to 0.8
        ),
        "HYBRID" to mapOf(
            "damagePerMinute" to 1.1,
            "crowdControlScore" to 1.1
        )
    )

    // Пороги MMR для рангов
    private val rankThresholds = mutableMapOf(
        "CHALLENGER" to 2500.0,
        "GRANDMASTER" to 2200.0,
        "MASTER" to 2000.0,
        "DIAMOND" to 1800.0,
        "PLATINUM" to 1600.0,
        "GOLD" to 1400.0,
        "SILVER" to 1200.0,
        "BRONZE" to 1000.0,
        "IRON" to 0.0
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

    fun getTopsFromParticipant(part: ParticipantsNew): Int {
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

        val detectedRole = detectRoleByStats(player)

        // Получение весов с учетом роли
        val roleWeights = getWeightsForRole(detectedRole)

        val topStats = getTopsFromParticipant(player)
        textAllResult += "[topStats]:{$topStats};"

        // Расчет Performance Score
        val performanceScore = calculatePerformanceScore(player, roleWeights).to1Digits() + (topStats * 0.4)

        // Расчет изменения MMR
        val kFactor = baseKFactor

        val (expectedWin, actualResult) = calculateWinProbability(teamMMR, enemyTeamMMR, matchResult)
        textAllResult += "[expectedWin, actualResult]:{$expectedWin, $actualResult};"
        val mmrChange = (kFactor * (actualResult - expectedWin) * (1.0 + performanceImpact * (performanceScore - 1.0))).to1Digits()
        val newMMR = (currentMMR + mmrChange).to1Digits()

        // Определение ранга и оценки
        val rank = determineRank(newMMR)
        val matchGrade = calculateMatchGrade(performanceScore)

        return AramMatchResult(
            newMMR = newMMR,
            mmrChange = mmrChange,
            performanceScore = performanceScore,
            matchGrade = matchGrade,
            rank = rank,
            adjustedStats = player,
            detectedRole = detectedRole,
            lolName = player.riotIdGameName,
            lolChampion = player.championName,
            topsStats = topStats,
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

        val damageMitidated = player.damageSelfMitigated.toDouble().to1Digits() / timeFactor
        textAllResult += "[damageMitidated]:{${damageMitidated}}; "

        val skillAccuracy = (player.skillshotsHit.toDouble() - player.skillshotsDodged).toInt()
        textAllResult += "[skillAccuracy]:{${skillAccuracy}}; "

        return when {
            // Танк - много CC и выживаний
            ccRatio > 50 && damageMitidated > 10000 && skillAccuracy < 50 -> "TANK"
            // Саппорт - много хила и CC
            damageRatio < 1100 && healTotal > 12000 && ccRatio > 30 && skillAccuracy > 20 -> "SUPPORT"
            // Дамагер - высокий урон, мало хила
            damageRatio > 2000 && healRatioTeam < 3000 && damageMitidated < 8000 && skillAccuracy > 10 -> "DAMAGE"
            // Дамагер - высокий урон, мало хила
            damageRatio > 2000 && healRatioTeam < 3000 && damageMitidated > 15000 -> "BROUSER"
            // Бесполезный - мало всего
            damageRatio < 1000 && ccRatio < 40 && healTotal < 12000 -> "USUSLESS"
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
        val normalKDA = 6.0
        val normalizedKDA = normalize(player.kda, 0.0, normalKDA)
        textAllResult += "[kda]:{$normalizedKDA, ${player.kda}, need:$normalKDA}; "

        val normalKillQueue = 1.0
        val normalizedKillQueue = normalize(player.kills4 * 0.4 + player.kills5, 0.0, normalKillQueue)
        textAllResult += "[normalizedKillQueue]:{$normalizedKillQueue, ${player.kills4 * 0.4 + player.kills5}, need:$normalKillQueue}; "

        val normalDPM = 2000.0
        val normalizedDPM = normalize(player.damagePerMinute, 0.0, normalDPM * timeFactor)
        textAllResult += "[damagePerMinute]:{$normalizedDPM, ${player.damagePerMinute}, need:${normalDPM * timeFactor}}; "

        val normalMitigated = 50000.0
        val normalizedDmgMitigated = normalize(player.damageSelfMitigated.toDouble(), 0.0, normalMitigated * timeFactor)
        textAllResult += "[damageSelfMitigated]:{$normalizedDmgMitigated, ${player.damageSelfMitigated.toDouble().to1Digits()}, need:${normalMitigated * timeFactor}}; "

        val normalKP = 1.0
        val normalizedKP = player.killParticipation.coerceIn(0.0, 1.0)
        textAllResult += "[killParticipation]:{$normalizedKP, ${player.killParticipation}, need:${normalKP}}; "

        val normalHealTeam = 15000.0
        val normalizedHealTeam = normalize(player.totalHealsOnTeammates.toDouble().to1Digits(), 0.0, normalHealTeam * timeFactor)
        textAllResult += "[totalHealsOnTeammates]:{$normalizedHealTeam, ${player.totalHealsOnTeammates.toDouble().to1Digits()}, need:${normalHealTeam * timeFactor}}; "

        val normalCC = 70.0
        val normalizedCC = normalize(player.timeCCingOthers.toDouble(), 0.0, normalCC * timeFactor)
        textAllResult += "[timeCCingOthers]:{$normalizedCC, ${player.timeCCingOthers.toDouble().to1Digits()}, need:${normalCC * timeFactor}}; "

        val normalGoldPM = 1000.0
        val normalizedGPM = normalize(player.goldPerMinute, 0.0, normalGoldPM * timeFactor)
        textAllResult += "[goldPerMinute]:{$normalizedGPM, ${player.goldPerMinute}, need:${normalGoldPM * timeFactor}}; "

        val normalObjectives = 2.0
        val normalizedObjectives = normalize(player.turretTakedowns.toDouble(), 0.0, normalObjectives)
        textAllResult += "[turretTakedowns]:{$normalizedObjectives, ${player.turretTakedowns}, need:${normalObjectives}}; "

        val normalSurvLowHP = 2.0
        val survivedLowHP = normalize(player.survivedSingleDigitHpCount.toDouble() + player.tookLargeDamageSurvived * 0.5, 0.0, normalSurvLowHP * timeFactor)
        textAllResult += "[survivedLowHP]:{$survivedLowHP, ${(player.survivedSingleDigitHpCount.toDouble() + player.tookLargeDamageSurvived * 0.5).to1Digits()}, need:${normalSurvLowHP * timeFactor}}; "

        val normalAccuracy = 30.0
        val skillAccuracy = normalize(player.skillshotsHit.toDouble() - player.skillshotsDodged, 0.0, normalAccuracy * timeFactor)
        textAllResult += "[skillAccuracy]:{$skillAccuracy, ${(player.skillshotsHit.toDouble() - player.skillshotsDodged).to1Digits()}, need:${normalAccuracy * timeFactor}}; "

        val normalOutFights = 4.0
        val outnumberedFights = normalize(player.outnumberedKills.toDouble() * 1.5 + player.soloKills * 0.3, 0.0, normalOutFights * timeFactor)
        textAllResult += "[outnumberedFights]:{$outnumberedFights, ${(player.outnumberedKills.toDouble() * 1.5 + player.soloKills * 0.3).to1Digits()}, need:${normalOutFights * timeFactor}}; "

        // Взвешенная сумма всех параметров
        return (normalizedKDA * weights["kda"]!! +
                normalizedDPM * weights["damagePerMinute"]!! +
                normalizedKP * weights["killParticipation"]!! +
                normalizedHealTeam * weights["healTeam"]!! +
                normalizedCC * weights["crowdControlScore"]!! +
                normalizedObjectives * weights["objectiveParticipation"]!! +
                normalizedGPM * weights["goldPerMinute"]!! +
                survivedLowHP * weights["survivedLowHP"]!! +
                skillAccuracy * weights["skillAccuracy"]!! +
                outnumberedFights * weights["outnumberedFights"]!! +
                normalizedKillQueue * weights["killQueue"]!! +
                normalizedDmgMitigated * weights["DmgMitigated"]!!)
    }

    /**
     * Определяет ранг игрока по MMR.
     */
    private fun determineRank(mmr: Double): String {
        return rankThresholds.entries.firstOrNull { (_, threshold) -> mmr >= threshold }?.key ?: "IRON"
    }

    /**
     * Рассчитывает оценку за матч (S+, A, B и т.д.).
     */
    private fun calculateMatchGrade(performanceScore: Double): String {
        return when {
            performanceScore >= 8 -> "S+"
            performanceScore >= 7 -> "S"
            performanceScore >= 6 -> "S-"
            performanceScore >= 5 -> "A"
            performanceScore >= 4 -> "B"
            performanceScore >= 3 -> "C"
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
    private fun normalize(value: Double, min: Double, max: Double): Double {
        return ((value - min) / (max - min)).coerceIn(0.0, 1.0).to1Digits()
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
    val topsStats: Int,
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
                "topsStats=$topsStats, " +
                "textResult='${textResult.split(";").joinToString("\t\n")}')"
    }
}