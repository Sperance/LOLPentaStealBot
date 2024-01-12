package ru.descend.bot.postgre

import Entity
import databases.Database
import delete
import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.UserData
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import ru.descend.bot.toStringUID
import table

data class TableKORD_LOL(
    override var id: Int = 0,

    var KORDperson: TableKORDPerson? = null,
    var LOLperson: TableLOLPerson? = null,
    var guild: TableGuild? = null
): Entity() {
    val messages: List<TableMessage> by oneToMany(TableMessage::KORD_LOL)

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

    fun asUser(guild: Guild) : User {
        return User(UserData(Snowflake(KORDperson!!.KORD_id.toLong()), KORDperson!!.KORD_name, KORDperson!!.KORD_discriminator), guild.kord)
    }
}

val tableKORDLOL = table<TableKORD_LOL, Database>()