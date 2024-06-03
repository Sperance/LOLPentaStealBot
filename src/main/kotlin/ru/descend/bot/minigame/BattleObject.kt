package ru.descend.bot.minigame

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class BattleObject(private val person1: Person, private val person2: Person) {

    private var isFighting = false
    private var periodBattle = 100L
    private var timeBattle = 0L
    private var countAttack1 = 0L
    private var countAttack2 = 0L

    private fun initForBattle() {
        person1.initForBattle()
        person1.listeners.onStartBattle.invokeEach(person2, this)
        person2.initForBattle()
        person2.listeners.onStartBattle.invokeEach(person1, this)
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
                if (onAttacking(person1, person2)) countAttack1++
                timerFlow1 = 0.0
            }
            if (timerFlow2 >= person2.stats.attackSpeed.get()) {
                if (onAttacking(person2, person1)) countAttack2++
                timerFlow2 = 0.0
            }

            if (!person1.personBlobs.isAlive.get()) {
                println("${person1.name} is DIED")
                isFighting = false
            }
            if (!person2.personBlobs.isAlive.get()) {
                println("${person2.name} is DIED")
                isFighting = false
            }
            timeBattle += periodBattle
        }
    }

    private fun onAttacking(current: Person, enemy: Person) : Boolean {
        if (!current.personBlobs.isAccessAttack.get()) return false
        current.listeners.onBeforeDamage.invokeEach(enemy, this)
        val fromAttack = current.stats.attack.get()
        enemy.stats.health.remStock(fromAttack)
        println("${current.name} атаковал ${enemy.name} на $fromAttack со скоростью ${current.stats.attackSpeed.get()}. Текущее ХП: ${current.stats.health.get()} Осталось жизней у ${enemy.name} : ${enemy.stats.health.get()}")
        println("person: ${current.stats.health}")
        println("item: ${(current.items.first() as EquipItem).stats}\n")
        current.listeners.onDealDamage.invokeEach(enemy, this)
        enemy.listeners.onTakeDamage.invokeEach(current, this)
        return true
    }

    private fun tickerFlow(period: Long) = flow {
        while (true) {
            if (!isFighting) return@flow
            emit(Unit)
            delay(period)
        }
    }

    /**************************************/

    fun calculateTimeBattle() = timeBattle

    fun calculateCountAttack(person: Person) : Long {
        if (person1.name == person.name) return countAttack1
        else if (person2.name == person.name) return countAttack2
        return -1
    }
}