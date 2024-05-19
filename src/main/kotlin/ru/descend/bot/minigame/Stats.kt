package ru.descend.bot.minigame

interface IntStatBattle {
    fun initForBattle()
}

/**
 * Здоровье персонажа
 */
class Health(
    val person: Person,
    val battleStat: BattleProperty = BattleProperty("Здоровье")
) : Property("Здоровье"), IntStatBattle {
    init {
        battleStat.addOnChangeListener {
            if (it.get() <= person.personValues.lowLevelHealth.value) {
                it.set(person.personValues.lowLevelHealth.value, false)
                person.personBlobs.isAlive.set(false)
            }
        }
    }

    override fun initForBattle() {
        battleStat.set(get())
    }
}

/**
 * Атака персонажа
 */
class Attack(
    val person: Person,
    val battleStat: BattleProperty = BattleProperty("Урон")
) : Property("Урон"), IntStatBattle {
    override fun initForBattle() {
        battleStat.set(get())
    }
}

/**
 * Скорость атаки персонажа
 */
class AttackSpeed(
    val person: Person,
    val battleStat: BattleProperty = BattleProperty("Скорость атаки")
) : Property("Скорость атаки"), IntStatBattle {
    override fun initForBattle() {
        var setting = get()
        if (setting < person.personValues.maxAttackSpeed.value) setting = person.personValues.maxAttackSpeed.value
        battleStat.set(setting)
    }
}

data class PersonStats (
    val person: Person,
    val health: Health = Health(person),
    val attack: Attack = Attack(person),
    val attackSpeed: AttackSpeed = AttackSpeed(person)
) {
    fun initForBattle() {
        health.initForBattle()
        attack.initForBattle()
        attackSpeed.initForBattle()
    }
}