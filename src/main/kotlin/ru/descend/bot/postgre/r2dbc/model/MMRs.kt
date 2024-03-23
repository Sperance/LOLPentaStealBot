package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.interfaces.InterfaceR2DBC
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.calculateUpdate

val tbl_MMRs = Meta.mmRs

@KomapperEntity
@KomapperTable("tbl_MMRs")
data class MMRs(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

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
) : InterfaceR2DBC<MMRs> {

    override suspend fun save() : MMRs {
        val result = R2DBC.runQuery(QueryDsl.insert(tbl_MMRs).single(this@MMRs))
        printLog("[MMRs::save] $result")
        return result
    }

    override suspend fun update() : MMRs {
        val before = this
        val after = R2DBC.runQuery(QueryDsl.update(tbl_MMRs).single(this@MMRs))
        printLog("[MMRs::update] $this { ${calculateUpdate(before, after)} }")
        return after
    }

    override suspend fun delete() {
        printLog("[MMRs::delete] $this")
        R2DBC.runQuery(QueryDsl.delete(tbl_MMRs).single(this@MMRs))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MMRs

        if (id != other.id) return false
        if (champion != other.champion) return false
        if (matchDuration != other.matchDuration) return false
        if (minions != other.minions) return false
        if (skills != other.skills) return false
        if (shielded != other.shielded) return false
        if (healed != other.healed) return false
        if (dmgBuilding != other.dmgBuilding) return false
        if (controlEnemy != other.controlEnemy) return false
        if (skillDodge != other.skillDodge) return false
        if (immobiliz != other.immobiliz) return false
        if (dmgTakenPerc != other.dmgTakenPerc) return false
        if (dmgDealPerc != other.dmgDealPerc) return false
        if (kda != other.kda) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + champion.hashCode()
        result = 31 * result + matchDuration.hashCode()
        result = 31 * result + minions.hashCode()
        result = 31 * result + skills.hashCode()
        result = 31 * result + shielded.hashCode()
        result = 31 * result + healed.hashCode()
        result = 31 * result + dmgBuilding.hashCode()
        result = 31 * result + controlEnemy.hashCode()
        result = 31 * result + skillDodge.hashCode()
        result = 31 * result + immobiliz.hashCode()
        result = 31 * result + dmgTakenPerc.hashCode()
        result = 31 * result + dmgDealPerc.hashCode()
        result = 31 * result + kda.hashCode()
        return result
    }

    override fun toString(): String {
        return "MMRs(id=$id, champion='$champion')"
    }
}