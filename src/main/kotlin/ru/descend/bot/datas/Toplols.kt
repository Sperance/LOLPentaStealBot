package ru.descend.bot.datas

import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.to1Digits

// Immutable data class для хранения данных о герое и статистике
data class TopLolObject(
    val lolId: Int,
    val statName: String,
    val statValue: Double,
    val statLolName: String = "",
    val matchText: String = ""
) {
    override fun toString(): String = "$statValue $statLolName"
}

class Toplols {
    // Храним топ-3 героя для каждой статистики
    private val topStats = mutableMapOf<String, List<TopLolObject>>()

    fun calculateField(lol: LOLs, statName: String, value: Double) {
        var disStatName = "**$statName**"
        val currentTop = topStats[disStatName]?.toMutableList() ?: mutableListOf()

        // Добавляем нового героя или обновляем существующего
        val existingIndex = currentTop.indexOfFirst { it.lolId == lol.id }

        if (existingIndex >= 0) {
            // Обновляем существующую запись если новое значение лучше
            if (currentTop[existingIndex].statValue < value.to1Digits()) {
                currentTop[existingIndex] = currentTop[existingIndex].copy(statValue = value.to1Digits())
            }
        } else {
            // Добавляем нового героя
            currentTop.add(TopLolObject(lol.id, disStatName, value.to1Digits(), lol.getCorrectNameWithTag()))
        }

        // Сортируем по убыванию и оставляем топ-3
        topStats[disStatName] = currentTop
            .sortedByDescending { it.statValue }
            .take(3)
    }

    // Метод для получения топ-3 по конкретной статистике
    fun getTopAll() = topStats
}