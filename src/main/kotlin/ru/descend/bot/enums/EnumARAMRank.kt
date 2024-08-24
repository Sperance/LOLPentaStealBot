package ru.descend.bot.enums

private const val modRank = 150.0
private const val modTitle = 200.0

enum class EnumARAMRank(val nameRank: String, val minMMR: Double, val rankValue: Int) {
    UNRANKED("Нет ранга", 0.0, 0),
    IRON_III("Железо III", UNRANKED.minMMR + modTitle, 1),
    IRON_II("Железо II", IRON_III.minMMR + modRank, 1),
    IRON_I("Железо I", IRON_II.minMMR + modRank, 1),
    BRONZE_III("Бронза III", IRON_I.minMMR + modTitle, 2),
    BRONZE_II("Бронза II", BRONZE_III.minMMR + modRank, 2),
    BRONZE_I("Бронза I", BRONZE_II.minMMR + modRank, 2),
    SILVER_III("Серебро III", BRONZE_I.minMMR + modTitle, 3),
    SILVER_II("Серебро II", SILVER_III.minMMR + modRank, 3),
    SILVER_I("Серебро I", SILVER_II.minMMR + modRank, 3),
    GOLD_III("Золото III", SILVER_I.minMMR + modTitle, 4),
    GOLD_II("Золото II", GOLD_III.minMMR + modRank, 4),
    GOLD_I("Золото I", GOLD_II.minMMR + modRank, 4),
    PLATINUM_III("Платина III", GOLD_I.minMMR + modTitle, 5),
    PLATINUM_II("Платина II", PLATINUM_III.minMMR + modRank, 5),
    PLATINUM_I("Платина I", PLATINUM_II.minMMR + modRank, 5),
    DIAMOND_III("Алмаз III", PLATINUM_I.minMMR + modTitle, 6),
    DIAMOND_II("Алмаз II", DIAMOND_III.minMMR + modRank, 6),
    DIAMOND_I("Алмаз I", DIAMOND_II.minMMR + modRank, 6),
    MASTER_III("Мастер III", DIAMOND_I.minMMR + modTitle, 7),
    MASTER_II("Мастер II", MASTER_III.minMMR + modRank, 7),
    MASTER_I("Мастер I", MASTER_II.minMMR + modRank, 7),
    CHALLENGER("Челленджер", MASTER_I.minMMR + modTitle, 8)
    ;

    companion object {
        fun getMMRRank(mmr: Double) : EnumARAMRank {
            return entries.sortedBy { it.minMMR }.firstOrNull { it.minMMR >= mmr } ?: entries.last()
        }
    }
}