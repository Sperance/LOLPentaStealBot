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
//        pers1.personBlobs.isAccessAttack.set(false)
        val pers2 = Person("Enemy")
        pers2.stats.attackSpeed.setStock(700)
        pers2.stats.health.setStock(1200)
        pers2.stats.attack.setStock(14)
        pers2.effects.addEffect(newEffectDMG)
        pers2.personBlobs.isAccessAttack.set(false)

        val battle = BattleObject(pers1, pers2)
        runBlocking {
            battle.doBattle()
        }
    }
}