package ru.descend.bot.minigame

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class BattleObject(private val person1: Person, private val person2: Person) {

    private var isFighting = false

    private fun initForBattle() {
        person1.initForBattle()
        person2.initForBattle()
    }

    suspend fun doBattle() {
        isFighting = true
        initForBattle()
        tickerFlow(100).collect {
            checkDealAttack(person1) {
                attackedObject(person1, person2)
            }
            checkDealAttack(person2) {
                attackedObject(person2, person1)
            }
        }
    }

    private fun checkDealAttack(person: Person, onAttack: () -> Unit) {
        person.stats.attackSpeed.battleStat.rem(100)
        if (person.stats.attackSpeed.battleStat.get() <= 0.0) {
            person.stats.attackSpeed.initForBattle()
            onAttack.invoke()
        }
    }

    private fun attackedObject(from: Person, to: Person) {
        val fromAttack = from.stats.attack.battleStat.get()
        to.stats.health.battleStat.rem(fromAttack)
        println("${from.name} атаковал ${to.name} на $fromAttack. Осталось жизней у ${to.name} : ${to.stats.health.battleStat.get()}")
        if (!to.personBlobs.isAlive.get()) {
            println("person ${to.name} is DIED")
            isFighting = false
        }
    }

    private fun tickerFlow(period: Long) = flow {
        while (true) {
            if (!isFighting) return@flow
            emit(Unit)
            delay(period)
        }
    }
}