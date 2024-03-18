package ru.descend.bot.postgre.tables

import Entity
import column
import databases.Database
import delete
import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.UserData
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import org.junit.jupiter.params.aggregator.ArgumentAccessException
import ru.descend.bot.toStringUID
import table

data class TableKORD_LOL(
    override var id: Int = 0,
    var mmrAram: Double = 0.0,
    var mmrAramSaved: Double = 0.0,

    var KORDperson: TableKORDPerson? = null,
    var LOLperson: TableLOLPerson? = null,
    var guild: TableGuild? = null
): Entity() {

    companion object {

        fun getForKORD(user: User) : Pair<TableKORDPerson, List<TableKORD_LOL>>? {
            val KORD = tableKORDPerson.first { TableKORDPerson::KORD_id eq user.toStringUID() } ?: return null
            val KORDLOL = tableKORDLOL.getAll { TableKORD_LOL::KORDperson eq KORD }
            return Pair(KORD, KORDLOL)
        }

        fun deleteForKORD(id: Int) {
            tableKORDPerson.getAll { TableKORDPerson::id eq id }.forEach { KORDpt ->
                tableKORDLOL.getAll { TableKORD_LOL::KORDperson eq KORDpt }.forEach { KORDLOLpt ->
                    KORDLOLpt.delete()
                }
                KORDpt.delete()
            }
        }

        fun deleteForLOL(id: Int) {
            tableLOLPerson.getAll { TableLOLPerson::id eq id }.forEach { LOLpt ->
                tableKORDLOL.getAll { TableKORD_LOL::LOLperson eq LOLpt }.forEach { KORDLOLpt ->
                    KORDLOLpt.delete()
                }
            }
        }
    }

    fun getNickName() : String {
        if (LOLperson == null) return ""
        return if (LOLperson?.LOL_riotIdName.isNullOrEmpty()) LOLperson?.LOL_summonerName?:""
        else LOLperson?.LOL_riotIdName?:""
    }

    fun getNickNameWithTag() : String {
        return getNickName() + "#" + LOLperson?.LOL_riotIdTagline
    }

    fun asUser(guild: Guild) : User {
        if (KORDperson == null) throw ArgumentAccessException("KORDperson is NULL. KORDLOL_id: $id")
        return User(UserData(Snowflake(KORDperson!!.KORD_id.toLong()), KORDperson!!.KORD_name, KORDperson!!.KORD_discriminator), guild.kord)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TableKORD_LOL

        if (id != other.id) return false
        if (mmrAram != other.mmrAram) return false
        if (mmrAramSaved != other.mmrAramSaved) return false
        if (KORDperson != other.KORDperson) return false
        if (LOLperson != other.LOLperson) return false
        if (guild != other.guild) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + mmrAram.hashCode()
        result = 31 * result + mmrAramSaved.hashCode()
        result = 31 * result + (KORDperson?.hashCode() ?: 0)
        result = 31 * result + (LOLperson?.hashCode() ?: 0)
        result = 31 * result + (guild?.hashCode() ?: 0)
        return result
    }


}

val tableKORDLOL = table<TableKORD_LOL, Database> {
    column(TableKORD_LOL::guild).check { it neq null }
    column(TableKORD_LOL::LOLperson).check { it neq null }
    column(TableKORD_LOL::KORDperson).check { it neq null }
}