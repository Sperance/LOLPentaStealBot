package ru.descend.bot.postgre.tables

import Entity
import databases.Database
import table

data class TableMmr(
    override var id: Int = 0,
    var champion: String = "",
    var matchDuration: Double = 0.0,
    var minions: Double = 0.0,
    var skills: Double = 0.0,
    var shielded: Double = 0.0,
    var healed: Double = 0.0,
    var dmgBuilding: Double = 0.0,
    var controlEnemy: Double = 0.0,
    var skillDodge: Double = 0.0,
    var immobiliz: Double = 0.0,
    var dmgTakenPerc: Double = 0.0,
    var dmgDealPerc: Double = 0.0,
    var kda: Double = 0.0
) : Entity()

val tableMmr = table<TableMmr, Database>()