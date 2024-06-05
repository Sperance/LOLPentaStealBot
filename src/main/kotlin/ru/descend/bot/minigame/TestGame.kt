package ru.descend.bot.minigame

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.komapper.core.dsl.QueryDsl
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.R2DBC.db
import ru.descend.bot.postgre.r2dbc.model.GamePersons
import ru.descend.bot.postgre.r2dbc.model.GamePersons.Companion.tbl_gamepersons
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.Heroes
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants

class TestGame {

    @Test
    fun testGet() {
        runBlocking {
            val pers = db.withTransaction { db.runQuery { QueryDsl.from(tbl_gamepersons).where { tbl_gamepersons.id eq 1 } } }.first()
            println("PERS OBJ: ${pers.encodeToPerson()}")
        }
    }

    @Test
    fun test1() {
        val pers1 = Person("Hero")
        pers1.stats.attackSpeed.setStock(1500)
        pers1.stats.health.setStock(1000)
        pers1.stats.health.stockPercent = 10.0
        pers1.stats.attack.setStock(33)

        val item = EquipItem("Меч героя")
        item.isEquipped = true
        item.stats.add(StockProperty(name = EnumPropName.HEALTH, value = 200.0, itemPercent = 50.0, stockPercent = 10.0))
        pers1.items.add(item)

        val newEffectHeal = BaseApplyEffect("Новый эф", listOf(
            EffectHeal(10.0, EnumPersonLifects.ON_TAKE_DAMAGE),
            EffectAdditionalDamage(10.0, EnumPersonLifects.ON_DEAL_DAMAGE),
            EffectDoubleDamageEveryAttack(3)
        ))

        val newEffectDMG = BaseApplyEffect("Новый эф2", listOf(
//            EffectAdditionalDamage(10.0, EnumPersonLifects.ON_TAKE_DAMAGE),
            EffectAttackSpeedUP(25.0, EnumPersonLifects.ON_DEAL_DAMAGE)
        ))

        pers1.effects.addEffect(newEffectHeal)

        runBlocking {
            R2DBC.db.withTransaction {
                R2DBC.db.runQuery { QueryDsl.create(tbl_gamepersons) }
            }
            GamePersons().createPerson(pers1)
        }

//        val serial = Json.encodeToString(pers1)
//        println(serial)
//        val newPers = Json.decodeFromString<Person>(serial)
//        println(Json.encodeToString(newPers))

//        pers1.personBlobs.isAccessAttack.set(false)
//        val pers2 = Person("Enemy")
//        pers2.stats.attackSpeed.setStock(700)
//        pers2.stats.health.setStock(1200)
//        pers2.stats.attack.setStock(14)
//        pers2.effects.addEffect(newEffectDMG)
//        pers2.personBlobs.isAccessAttack.set(false)
//
//        val battle = BattleObject(pers1, pers2)
//        runBlocking {
//            battle.doBattle()
//        }
    }
}