package ru.descend.derpg

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.json.contains
import org.jetbrains.exposed.v1.json.exists
import ru.descend.bot.printLog
import ru.descend.derpg.DatabaseConfig.dbQuery
import ru.descend.derpg.data.characters.CharacterParams
import ru.descend.derpg.data.characters.CharactersTable
import ru.descend.derpg.data.characters.DAOCharacters
import ru.descend.derpg.data.characters.EnumStatBool
import ru.descend.derpg.data.characters.EnumStatKey
import ru.descend.derpg.data.characters.EnumStatType
import ru.descend.derpg.data.characters.Stat
import ru.descend.derpg.data.characters.StatBool
import ru.descend.derpg.data.characters.StatContainer
import ru.descend.derpg.data.equipments.DAOequipments
import ru.descend.derpg.data.users.DAOusers
import ru.descend.derpg.test.ItemObject

fun main() {
    runBlocking {
        DatabaseConfig.init()

        val userDao = DAOusers()
        val characterDao = DAOCharacters()
        val equipmentDao = DAOequipments()

        dbQuery {
            val user = userDao.create {
                name = "John${System.currentTimeMillis()}"
                email = "john@example.com"
            }

            characterDao.create {
                title = "First post"
                content = "Hello JSONB!"
                inventory = arrayListOf(ItemObject("Sword"), ItemObject("Arrow"))
                params = CharacterParams(inventory_size = 10)
//                stats = mutableSetOf(Stat(EnumStatKey.LIFE, 200.0), Stat(EnumStatKey.ATTACK_SPEED, 1.0))
                this.user = user
            }

            val statContainer = StatContainer(
                stats = mutableSetOf(Stat(EnumStatKey.LIFE, EnumStatType.FLAT, 100.0), Stat(EnumStatKey.LIFE, EnumStatType.PERCENT, 5.0), Stat(EnumStatKey.ATTACK_SPEED, EnumStatType.FLAT,2.0)),
                statsBool = mutableSetOf(StatBool(EnumStatBool.IS_BANNED, true), StatBool(EnumStatBool.IS_ALIVE, false))
            )

            val charEnt = characterDao.create {
                title = "Draft post"
                content = "Work in progress"
                inventory = arrayListOf(ItemObject("Body"))
                params = CharacterParams()
                stats = statContainer
                this.user = user
            }

             equipmentDao.create {
                name = "Sword of sandals1"
                content = "Description sample"
                metadata = arrayListOf(ItemObject("STATS"), ItemObject("asd"))
                this.character = charEnt
            }

             equipmentDao.create {
                name = "Sword of sandals2"
                content = "Description sample"
                metadata = arrayListOf(ItemObject("STATS"), ItemObject("532"))
                this.character = charEnt
            }
//
//             equipmentDao.create {
//                name = "Sword of sandals3"
//                content = "Description sample"
//                metadata = arrayListOf(ItemObject("STATS"), ItemObject("f34t"), ItemObject("34536s"))
//                this.character = charEnt
//            }
//
//            printLog("POSTS 1: ${user.getCharacters()}")
//
//            user.getCharacters().forEachIndexed { ind, it ->
//                it.inventory = it.inventory.toMutableList().apply { add(ItemObject("Ins: $ind")) }
//            }
//
//            val posts = user.getCharacters()
//
//            printLog("POSTS 2: $posts")
//
//            printLog("activeProjects: ${CharactersTable.selectAll().where { CharactersTable.inventory.contains("{\"name\":\"Sword\"}") }.count()}")
//
//            printLog(":::CHARACTERS:::")
//            user.getCharacters().forEach { char ->
//                printLog("\t" + char)
//                printLog("\t:::EQUIP:::")
//                char.getEquipments().forEach { equip ->
//                    equipmentDao.update(equip) {
//                        this.name = "CHANGED NMM"
//                    }
//                    printLog("\t\t" + equip)
//                }
//
//            }
        }
        DatabaseConfig.close()
    }
}