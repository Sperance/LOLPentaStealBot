package ru.descend.bot.postgre.calculating

import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import kotlin.math.pow

class Calc_MMRv3 {

    companion object {
        // Пороги для определения аномальных показателей
        private const val HIGH_DAMAGE_DEVIATION = 1.5  // Во сколько раз урон выше среднего для роли
        private const val HIGH_HEAL_DEVIATION = 1.8    // Во сколько раз лечение выше среднего
        private const val LOW_DAMAGE_DEVIATION = 0.5   // Во сколько раз урон ниже среднего
        private const val HYBRID_THRESHOLD = 0.7       // Порог для определения гибридной игры

        // Пороги времени игры (в минутах)
        private const val SHORT_GAME_THRESHOLD = 10.0
        private const val LONG_GAME_THRESHOLD = 30.0

        // Базовые значения для нормализации
        private const val AVG_DAMAGE_PER_MIN = 1000.0
        private const val AVG_HEAL = 5000.0
        private const val AVG_CC_TIME = 30.0
    }

    // Настройки системы MMR
    private val baseKFactor = 32.0
    private val minGamesForStableK = 50
    private val performanceImpact = 0.25

    // Базовые веса параметров
    private val baseWeights = mapOf(
        "kda" to 0.12,
        "damagePerMinute" to 0.18,
        "killParticipation" to 0.12,
        "visionScorePerMinute" to 0.08,
        "healAndShielding" to 0.09,
        "crowdControlScore" to 0.08,
        "objectiveParticipation" to 0.08,
        "goldPerMinute" to 0.07,
        "survivedLowHP" to 0.06,
        "skillAccuracy" to 0.05,
        "outnumberedFights" to 0.05,
        "effectiveMovement" to 0.02
    )

    // Модификаторы весов для разных ролей
    private val roleModifiers = mapOf(
        "DAMAGE" to mapOf(
            "damagePerMinute" to 1.5,
            "healAndShielding" to 0.3,
            "crowdControlScore" to 0.7
        ),
        "TANK" to mapOf(
            "damagePerMinute" to 0.8,
            "healAndShielding" to 0.5,
            "crowdControlScore" to 1.5,
            "survivedLowHP" to 1.3
        ),
        "SUPPORT" to mapOf(
            "damagePerMinute" to 0.6,
            "healAndShielding" to 1.8,
            "visionScorePerMinute" to 1.5,
            "crowdControlScore" to 1.4
        ),
        "HYBRID" to mapOf(
            "damagePerMinute" to 1.1,
            "healAndShielding" to 1.2,
            "crowdControlScore" to 1.1
        )
    )

    // Пороги MMR для рангов
    private val rankThresholds = mapOf(
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

    /**
     * Основной метод для расчета нового MMR после матча.
     *
     * @param player Статистика игрока
     * @param currentMMR Текущий MMR игрока
     * @param teamMMR MMR членов команды (включая игрока)
     * @param enemyTeamMMR MMR противников
     * @param matchResult Результат матча (true - победа)
     * @param totalGamesPlayed Общее количество сыгранных игр
     * @return AramMatchResult с полной информацией о расчете
     */
    fun calculateNewMMR(
        player: ParticipantsNew,
        currentMMR: Double,
        teamMMR: List<Double>,
        enemyTeamMMR: List<Double>,
        matchResult: Boolean,
        totalGamesPlayed: Int
    ): AramMatchResult {
        // 1. Корректировка статистики по времени игры
        val timeAdjustedStats = adjustStatsByGameTime(player)

        // 2. Определение роли чемпиона
        val detectedRole = detectChampionRole(timeAdjustedStats)

        // 3. Получение весов с учетом роли
        var roleWeights = getWeightsForRole(detectedRole)

        // 4. Проверка аномалий и корректировка весов
        roleWeights = adjustWeightsForAnomalies(timeAdjustedStats, detectedRole, roleWeights)

        // 5. Расчет Performance Score
        val performanceScore = calculatePerformanceScore(timeAdjustedStats, roleWeights)

        // 6. Расчет изменения MMR
        val kFactor = calculateKFactor(totalGamesPlayed)
        val (expectedWin, actualResult) = calculateWinProbability(teamMMR, enemyTeamMMR, matchResult)
        val mmrChange = kFactor * (actualResult - expectedWin) * (1.0 + performanceImpact * (performanceScore - 1.0))
        val newMMR = currentMMR + mmrChange

        // 7. Определение ранга и оценки
        val rank = determineRank(newMMR)
        val matchGrade = calculateMatchGrade(performanceScore, matchResult)

        return AramMatchResult(
            newMMR = newMMR,
            mmrChange = mmrChange,
            performanceScore = performanceScore,
            matchGrade = matchGrade,
            rank = rank,
            adjustedStats = timeAdjustedStats,
            detectedRole = detectedRole,
            adjustedWeights = roleWeights
        )
    }

    /**
     * Корректирует статистику с учетом длительности матча.
     * Чем короче игра, тем сильнее усиливаем показатели.
     */
    private fun adjustStatsByGameTime(player: ParticipantsNew): ParticipantsNew {
        val gameTimeMinutes = player.timePlayed / 60.0
        val timeFactor = when {
            gameTimeMinutes < SHORT_GAME_THRESHOLD -> 1.5
            gameTimeMinutes > LONG_GAME_THRESHOLD -> 0.8
            else -> 1.0
        }

        return player.copy(
            damagePerMinute = player.damagePerMinute * timeFactor,
            goldPerMinute = player.goldPerMinute * timeFactor,
            visionScorePerMinute = player.visionScorePerMinute * timeFactor,
            totalHeal = (player.totalHeal * timeFactor).toInt(),
            totalHealsOnTeammates = (player.totalHealsOnTeammates * timeFactor).toInt(),
            timeCCingOthers = (player.timeCCingOthers * timeFactor).toInt()
        )
    }

    /**
     * Определяет роль чемпиона по имени или статистике.
     */
    private fun detectChampionRole(player: ParticipantsNew): String {
        // Предопределенные роли для известных чемпионов
        val presetRoles = mapOf(
            "Soraka" to "SUPPORT", "Janna" to "SUPPORT",
            "Zed" to "DAMAGE", "Sion" to "TANK",
            "Karma" to "HYBRID", "Senna" to "HYBRID"
        )

        return presetRoles[player.championName] ?: detectRoleByStats(player)
    }

    /**
     * Определяет роль по статистическим показателям.
     */
    private fun detectRoleByStats(player: ParticipantsNew): String {
        val damageRatio = player.damagePerMinute / AVG_DAMAGE_PER_MIN
        val healRatio = (player.totalHeal + player.totalHealsOnTeammates) / AVG_HEAL
        val ccRatio = player.timeCCingOthers / AVG_CC_TIME

        return when {
            // Саппорт - много хила и CC
            healRatio > 1.5 && ccRatio > 1.2 -> "SUPPORT"
            // Дамагер - высокий урон, мало хила
            damageRatio > 1.5 && healRatio < 0.7 -> "DAMAGE"
            // Танк - много CC и выживаний
            ccRatio > 1.5 && player.survivedSingleDigitHpCount > 3 -> "TANK"
            // Гибрид - баланс урона и хила
            damageRatio > 1.0 && healRatio > 1.0 -> "HYBRID"
            else -> "HYBRID" // По умолчанию
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
     * Корректирует веса при обнаружении аномалий в статистике.
     */
    private fun adjustWeightsForAnomalies(player: ParticipantsNew, role: String, currentWeights: Map<String, Double>): Map<String, Double> {
        val newWeights = currentWeights.toMutableMap()
        val damageRatio = player.damagePerMinute / AVG_DAMAGE_PER_MIN
        val healRatio = (player.totalHeal + player.totalHealsOnTeammates) / AVG_HEAL

        when (role) {
            "SUPPORT" -> {
                // Саппорт наносит необычно высокий урон
                if (damageRatio > HIGH_DAMAGE_DEVIATION) {
                    newWeights["damagePerMinute"] = newWeights["damagePerMinute"]!! * 1.4
                    newWeights["healAndShielding"] = newWeights["healAndShielding"]!! * 0.8
                }
            }
            "DAMAGE" -> {
                // Дамагер много хиллит
                if (healRatio > HIGH_HEAL_DEVIATION && damageRatio < 1.2) {
                    newWeights["healAndShielding"] = newWeights["healAndShielding"]!! * 1.5
                    newWeights["damagePerMinute"] = newWeights["damagePerMinute"]!! * 0.7
                }
            }
            "TANK" -> {
                // Танк мало tankил, но много контролил
                if (player.timeCCingOthers > AVG_CC_TIME * 1.5 && player.totalDamageTaken < 20000) {
                    newWeights["crowdControlScore"] = newWeights["crowdControlScore"]!! * 1.3
                }
            }
        }

        // Обнаружение гибридного стиля игры
        if (damageRatio > HYBRID_THRESHOLD && healRatio > HYBRID_THRESHOLD &&
            role != "HYBRID") {
            newWeights["damagePerMinute"] = newWeights["damagePerMinute"]!! * 1.2
            newWeights["healAndShielding"] = newWeights["healAndShielding"]!! * 1.2
        }

        return newWeights
    }

    /**
     * Рассчитывает Performance Score (0.5-1.5) на основе статистики и весов.
     */
    private fun calculatePerformanceScore(
        player: ParticipantsNew,
        weights: Map<String, Double>
    ): Double {
        // Нормализация основных параметров
        val normalizedKDA = normalize(player.kda, 0.0, 10.0)
        val normalizedDPM = normalize(player.damagePerMinute, 0.0, 2500.0)
        val normalizedVSPM = normalize(player.visionScorePerMinute, 0.0, 3.5)
        val normalizedKP = player.killParticipation.coerceIn(0.0, 1.0)
        val normalizedHeal = normalize(
            (player.totalHeal + player.totalHealsOnTeammates).toDouble(),
            0.0,
            20000.0
        )
        val normalizedCC = normalize(player.timeCCingOthers.toDouble(), 0.0, 150.0)
        val normalizedGPM = normalize(player.goldPerMinute, 0.0, 1200.0)
        val normalizedObjectives = normalize(
            player.turretTakedowns.toDouble() + player.dragonTakedowns * 0.5,
            0.0,
            5.0
        )

        // Нормализация скрытых факторов
        val survivedLowHP = normalize(
            player.survivedSingleDigitHpCount.toDouble() +
                    player.tookLargeDamageSurvived * 0.5,
            0.0,
            5.0
        )
        val skillAccuracy = normalize(
            player.skillshotsHit.toDouble() - player.skillshotsDodged * 0.2,
            0.0,
            30.0
        )
        val outnumberedFights = normalize(
            player.outnumberedKills.toDouble() * 1.5 +
                    player.soloKills * 0.3,
            0.0,
            10.0
        )
        val effectiveMovement = normalize(
            player.dodgeSkillShotsSmallWindow.toDouble(),
            0.0,
            15.0
        )

        // Взвешенная сумма всех параметров
        return (normalizedKDA * weights["kda"]!! +
                normalizedDPM * weights["damagePerMinute"]!! +
                normalizedVSPM * weights["visionScorePerMinute"]!! +
                normalizedKP * weights["killParticipation"]!! +
                normalizedHeal * weights["healAndShielding"]!! +
                normalizedCC * weights["crowdControlScore"]!! +
                normalizedObjectives * weights["objectiveParticipation"]!! +
                normalizedGPM * weights["goldPerMinute"]!! +
                survivedLowHP * weights["survivedLowHP"]!! +
                skillAccuracy * weights["skillAccuracy"]!! +
                outnumberedFights * weights["outnumberedFights"]!! +
                effectiveMovement * weights["effectiveMovement"]!!)
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
    private fun calculateMatchGrade(performanceScore: Double, isWin: Boolean): String {
        val baseGrade = when {
            performanceScore >= 1.3 -> "S"
            performanceScore >= 1.1 -> "A"
            performanceScore >= 0.9 -> "B"
            performanceScore >= 0.7 -> "C"
            else -> "D"
        }

        return if (isWin) {
            when (baseGrade) {
                "S" -> "S+"
                else -> baseGrade
            }
        } else {
            when (baseGrade) {
                "S" -> "A"
                "A" -> "B"
                else -> baseGrade
            }
        }
    }

    /**
     * Рассчитывает K-factor с учетом количества сыгранных игр.
     */
    private fun calculateKFactor(totalGames: Int): Double {
        return if (totalGames >= minGamesForStableK) {
            baseKFactor * 0.6
        } else {
            baseKFactor * (1.2 - (totalGames.coerceIn(0, minGamesForStableK) / 100.0))
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
        return ((value - min) / (max - min)).coerceIn(0.0, 1.0)
    }
}

/**
 * Результат расчета MMR после матча.
 */
data class AramMatchResult(
    val newMMR: Double,                  // Новый MMR после матча
    val mmrChange: Double,               // Изменение MMR
    val performanceScore: Double,        // Оценка эффективности (0.5-1.5)
    val matchGrade: String,              // Оценка за матч (S+, A, B и т.д.)
    val rank: String,                    // Текущий ранг
    val adjustedStats: ParticipantsNew,  // Статистика с учетом времени игры
    val detectedRole: String,            // Определенная роль
    val adjustedWeights: Map<String, Double>  // Использованные веса параметров
)