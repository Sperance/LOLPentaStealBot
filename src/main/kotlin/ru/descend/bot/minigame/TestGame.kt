package ru.descend.bot.minigame

import kotlinx.coroutines.runBlocking
import org.junit.Test

class TestGame {

    @Test
    fun test1() {
        val pers1 = Person("Hero")
        pers1.stats.attackSpeed.set(1500)
        pers1.stats.health.set(1000)
        pers1.stats.attack.set(33)
        pers1.effects.addEffect(BaseEffectAdditionalDamage(20.0))
        val pers2 = Person("Enemy")
        pers2.stats.attackSpeed.set(100)
        pers2.stats.health.set(1200)
        pers2.stats.attack.set(14)

        val battle = BattleObject(pers1, pers2)
        runBlocking {
            battle.doBattle()
        }
    }

}