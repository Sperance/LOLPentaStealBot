package ru.descend.bot.enums

private const val modRank = 70.0
private const val modTitle = 100.0

enum class EnumMMRRank(val nameRank: String, val minMMR: Double, val rankValue: Int) {
    UNRANKED("Нет ранга", 0.0, 0),
    PAPER_III("Бумага III", UNRANKED.minMMR + modRank, 1),
    PAPER_II("Бумага II", PAPER_III.minMMR + modRank, 1),
    PAPER_I("Бумага I", PAPER_II.minMMR + modRank, 1),
    WOOD_III("Дерево III", PAPER_I.minMMR + modTitle, 2),
    WOOD_II("Дерево II", WOOD_III.minMMR + modRank, 2),
    WOOD_I("Дерево I", WOOD_II.minMMR + modRank, 2),
    IRON_III("Железо III", WOOD_I.minMMR + modTitle, 3),
    IRON_II("Железо II", IRON_III.minMMR + modRank, 3),
    IRON_I("Железо I", IRON_II.minMMR + modRank, 3),
    BRONZE_III("Бронза III", IRON_I.minMMR + modTitle, 4),
    BRONZE_II("Бронза II", BRONZE_III.minMMR + modRank, 4),
    BRONZE_I("Бронза I", BRONZE_II.minMMR + modRank, 4),
    SILVER_III("Серебро III", BRONZE_I.minMMR + modTitle, 5),
    SILVER_II("Серебро II", SILVER_III.minMMR + modRank, 5),
    SILVER_I("Серебро I", SILVER_II.minMMR + modRank, 5),
    GOLD_III("Золото III", SILVER_I.minMMR + modTitle, 6),
    GOLD_II("Золото II", GOLD_III.minMMR + modRank, 6),
    GOLD_I("Золото I", GOLD_II.minMMR + modRank, 6),
    PLATINUM_III("Платина III", GOLD_I.minMMR + modTitle, 7),
    PLATINUM_II("Платина II", PLATINUM_III.minMMR + modRank, 7),
    PLATINUM_I("Платина I", PLATINUM_II.minMMR + modRank, 7),
    DIAMOND_III("Алмаз III", PLATINUM_I.minMMR + modTitle, 8),
    DIAMOND_II("Алмаз II", DIAMOND_III.minMMR + modRank, 8),
    DIAMOND_I("Алмаз I", DIAMOND_II.minMMR + modRank, 8),
    MASTER_III("Мастер III", DIAMOND_I.minMMR + modTitle, 9),
    MASTER_II("Мастер II", MASTER_III.minMMR + modRank, 9),
    MASTER_I("Мастер I", MASTER_II.minMMR + modRank, 9),
    CHALLENGER("Челленджер", MASTER_I.minMMR + modTitle, 10)
    ;

    companion object {
        fun getMMRRank(mmr: Double) : EnumMMRRank {
            entries.forEach {
                if (it.minMMR >= mmr) return it
            }
            return UNRANKED
        }
    }
}