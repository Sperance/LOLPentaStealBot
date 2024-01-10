package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import delete
import table

data class FireKORD_LOLPersonTable(
    override var id: Int = 0,

    var KORDperson: FireKORDPersonTable? = null,
    var LOLperson: FireLOLPersonTable? = null
): Entity() {
    companion object {
        fun getForId(id: Int) : FireKORD_LOLPersonTable? {
            return fireKORD_LOLPersonTable.first { FireKORD_LOLPersonTable::id eq id }
        }

        fun deleteForKORD(id: Int) {
            fireKORDPersonTable.getAll { FireKORDPersonTable::id eq id }.forEach {KORDpt ->
                fireKORD_LOLPersonTable.getAll { FireKORD_LOLPersonTable::KORDperson eq KORDpt }.forEach {KORDLOLpt ->
                    KORDLOLpt.delete()
                }
            }
        }

        fun deleteForLOL(id: Int) {
            fireLOLPersonTable.getAll { FireLOLPersonTable::id eq id }.forEach {LOLpt ->
                fireKORD_LOLPersonTable.getAll { FireKORD_LOLPersonTable::LOLperson eq LOLpt }.forEach {KORDLOLpt ->
                    KORDLOLpt.delete()
                }
            }
        }
    }
}

val fireKORD_LOLPersonTable = table<FireKORD_LOLPersonTable, Database>()