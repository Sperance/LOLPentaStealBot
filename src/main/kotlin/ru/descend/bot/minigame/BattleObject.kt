package ru.descend.bot.minigame

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class BattleObject(private val person1: Person, private val person2: Person) {

    private var isFighting = false
    private var periodBattle = 100.0

    private fun initForBattle() {
        person1.initForBattle()
        person1.listeners.onStartBattle.invokeEach(person1, person2)
        person2.initForBattle()
        person2.listeners.onStartBattle.invokeEach(person2, person1)
    }

    suspend fun doBattle() {
        isFighting = true
        initForBattle()
        var timerFlow1 = 0.0
        var timerFlow2 = 0.0
        tickerFlow(periodBattle).collect {
            timerFlow1 += periodBattle
            timerFlow2 += periodBattle
            if (timerFlow1 >= person1.stats.attackSpeed.get()) {
                attackedObject(person1, person2)
                timerFlow1 = 0.0
            }
            if (timerFlow2 >= person2.stats.attackSpeed.get()) {
                attackedObject(person2, person1)
                timerFlow2 = 0.0
            }
        }
    }

    private fun attackedObject(from: Person, to: Person) {
        from.onAttacking(to)
        if (!to.personBlobs.isAlive.get()) {
            println("person ${to.name} is DIED")
            isFighting = false
        }
    }

    private fun tickerFlow(period: Double) = flow {
        while (true) {
            if (!isFighting) return@flow
            emit(Unit)
            delay(period.toLong())
        }
    }
}