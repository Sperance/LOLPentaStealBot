package ru.descend.bot.minigame

import kotlinx.coroutines.runBlocking
import org.junit.Test

class TestGame {

    @Test
    fun test1() {
        val pers1 = Person("Hero")
        pers1.stats.attackSpeed.setStock(1500)
        pers1.stats.health.setStock(1000)
        pers1.stats.attack.setStock(33)
        pers1.effects.addEffect(EffectSuperHeal())
        pers1.personBlobs.isAccessAttack.set(false)
//        pers1.effects.addEffect(BaseEffectAdditionalDamage(20.0))
        val pers2 = Person("Enemy")
        pers2.stats.attackSpeed.setStock(700)
        pers2.stats.health.setStock(1200)
        pers2.stats.attack.setStock(14)
        pers2.effects.addEffect(EffectSuperSpeedDMG())

        val battle = BattleObject(pers1, pers2)
        runBlocking {
            battle.doBattle()
        }
    }

}