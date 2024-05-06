package ru.descend.bot.minigame.gamedatas

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import ru.descend.bot.launch
import ru.descend.bot.minigame.BasePerson
import ru.descend.bot.printLog
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

class CalculateBattle(val person1: BasePerson, val person2: BasePerson) {

    private var isBattleProcess = false
    private var duration = 100.milliseconds
    private val ticker = tickerFlow(duration)

    init {
        person1.initForBattle()
        person2.initForBattle()
    }

    suspend fun doBattle() {
        isBattleProcess = true
        ticker.collect {
            if (!isBattleProcess) return@collect
            processedAttack(person1, person2)
            processedAttack(person2, person1)
        }
    }

    private fun onEndBattle(winner: BasePerson) {
        printLog("БОЙ ОКОНЧЕН. ПОБЕДИТЕЛЬ: ${winner.name} со здоровьем: ${winner.calcProps.health.getForBattle()}")
    }

    private fun processedAttack(personFrom: BasePerson, personTo: BasePerson) {
        personFrom.calcProps.attackSpeed.removeForBattle(duration.toDouble(DurationUnit.MILLISECONDS))
        if (personFrom.calcProps.attackSpeed.getForBattle() <= 0.0) {
            personFrom.onAttack(personTo)
            printLog("${personFrom.name} атаковал ${personTo.name}. Осталось жизней у ${personTo.name}: ${personTo.calcProps.health.getForBattle()}")
            if (personFrom.isDie() || personTo.isDie()) isBattleProcess = false
            personFrom.calcProps.attackSpeed.initForBattle()
        }
    }

    private fun tickerFlow(period: Duration) = flow {
        while (true) {
            if (!isBattleProcess) {
                onEndBattle(if (person1.isDie()) person2 else person1)
                return@flow
            }
            emit(Unit)
            delay(period)
        }
    }
}