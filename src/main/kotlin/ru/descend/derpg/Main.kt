package ru.descend.derpg

import kotlinx.coroutines.runBlocking
import ru.descend.bot.printLog
import ru.descend.derpg.DatabaseConfig.dbQuery
import ru.descend.derpg.data.characters.DAOCharacters
import ru.descend.derpg.data.equipments.DAOequipments
import ru.descend.derpg.data.equipments.SnapshotEquipment
import ru.descend.derpg.data.inventory.DAOinventory
import ru.descend.derpg.data.users.DAOusers
import ru.descend.derpg.enums.EnumEquipmentType
import ru.descend.derpg.enums.EnumItem
import ru.descend.derpg.enums.EnumStatBool
import ru.descend.derpg.enums.EnumStatKey
import ru.descend.derpg.enums.EnumStatType
import ru.descend.derpg.model.ItemStock
import ru.descend.derpg.model.ParamsStock
import ru.descend.derpg.model.Stat
import ru.descend.derpg.model.StatBool
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
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
                name = "Deascend"
                this.user = user
            }

            val charEnt = characterDao.create {
                name = "ATLANT"
                params = getStockParams()
                buffs = mutableSetOf(Stat(EnumStatKey.LIFE, EnumStatType.FLAT, 200.0), Stat(EnumStatKey.LIFE, EnumStatType.PERCENT, 20.0), Stat(EnumStatKey.ATTACK_SPEED, EnumStatType.FLAT,2.0))
                bools = mutableSetOf(StatBool(EnumStatBool.IS_BANNED, true), StatBool(EnumStatBool.IS_ALIVE, false))
                this.user = user
            }

            charEnt.setStat(StatBool(EnumStatBool.IS_BANNED, false))
            charEnt.setStat(ParamsStock(EnumStatKey.MAGIC_RESIST, 20.3))
            charEnt.setStat(ParamsStock(EnumStatKey.MAGIC_RESIST, 40.1))

            charEnt.setStat(ParamsStock(EnumStatKey.DEX, 3.0))
            charEnt.addStat(ParamsStock(EnumStatKey.DEX, 0.75))

            charEnt.remStat(ParamsStock(EnumStatKey.MAGIC_RESIST, 0.1))
            charEnt.remStat(ParamsStock(EnumStatKey.MAGIC_RESIST, 39.0))
            charEnt.remStat(ParamsStock(EnumStatKey.MAGIC_RESIST, 1.0))

            printLog(charEnt.getInventory().addItem(ItemStock(EnumItem.RESOURCE_WD, 400)))
            printLog(charEnt.getInventory().addItem(ItemStock(EnumItem.RESOURCE_WD, 4)))

            printLog(charEnt.getInventory().removeItem(ItemStock(EnumItem.RESOURCE_WD, 1)))
            printLog("INV: ${charEnt.getInventory()}")

            printLog(charEnt.getInventory().checkQuantity(ItemStock(EnumItem.RESOURCE_WD, 403)))
            printLog(charEnt.getInventory().checkQuantity(ItemStock(EnumItem.RESOURCE_WD, 404)))
            printLog(charEnt.getInventory().checkQuantity(ItemStock(EnumItem.RESOURCE_WD, 0)))
            printLog(charEnt.getInventory().checkQuantity(ItemStock(EnumItem.RESOURCE_WD, 1000)))

            printLog(charEnt.getInventory().setItem(ItemStock(EnumItem.RESOURCE_WD, 10)))
            printLog(charEnt.getInventory().setItem(ItemStock(EnumItem.ITEM_POTION_HEALTH, 4)))

            repeat(10) { counter ->
                printLog(charEnt.getInventory().addEquipment(SnapshotEquipment(_name = "name$counter", _content = "content$counter", _enumEquipmentType = EnumEquipmentType.GLOVES)))
            }

            val charSnap = charEnt.toSnapshot()

            val res1 = charSnap.calculateParamsWithBuffs()
            printLog("res1: $res1")

            val res2 = charSnap.calculateParamsWithBuffs()
            printLog("res2: $res2")

            printLog(":::EQUIPMENTS:::")
            printLog("${charEnt.getEquipments()}")

            printLog(":::EQUIPMENTS:::")
            printLog("${charEnt.getEquipments()}")
        }
        DatabaseConfig.close()
    }
}