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
        battleStat.onChangeListeners.addListener {
            if (it.get() <= it.minimumValue) {
                person.personBlobs.isAlive.set(false)
                person.listeners.onDie.invokeEach(person, null)
            }
        }
    }

    override fun initForBattle() {
        battleStat.minimumValue = 0.0
        battleStat.maximumValue = get()
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
        var settingValue = get()
        if (!person.personBlobs.enableCONSTmaxAttackSpeed.get() && settingValue < person.personValues.maxAttackSpeed.value) settingValue = person.personValues.maxAttackSpeed.value
        if (person.personBlobs.enableCONSTmaxAttackSpeed.get() && settingValue < person.personValues.CONSTmaxAttackSpeed.value) settingValue = person.personValues.CONSTmaxAttackSpeed.value
        battleStat.set(settingValue)
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

enum class EnumPersonLifects {
    ON_START_BATTLE,
    ON_DIE
}

data class PersonListeners (
    val person: Person,

    val onStartBattle: PersonListener = PersonListener("Начало боя", EnumPersonLifects.ON_START_BATTLE),
//    val onBeforeAttack: PersonListener = PersonListener("Перед атакой"),
//    val onAfterAttack: PersonListener = PersonListener("После атаки"),
//    val onAttacking: PersonListener = PersonListener("Атаковал"),
//    val onAttacked: PersonListener = PersonListener("Был атакован"),
    val onDie: PersonListener = PersonListener("Умер", EnumPersonLifects.ON_DIE)
)