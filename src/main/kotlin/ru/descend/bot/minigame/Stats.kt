package ru.descend.bot.minigame

interface IntStatBattle {
    fun initForBattle()
}

/**
 * Здоровье персонажа
 */
class Health(val person: Person) : Property("Здоровье"), IntStatBattle {
    init {
        onChangeListeners.addListener {
            if (get() <= minimumValue) {
                person.personBlobs.isAlive.set(false)
                person.listeners.onDie.invokeEach(person, null)
            }
        }
    }

    override fun initForBattle() {
        maximumValue = get()
        setStock(maximumValue!!)
    }
}

/**
 * Атака персонажа
 */
class Attack(val person: Person) : Property("Урон"), IntStatBattle {
    override fun initForBattle() {

    }
}

/**
 * Скорость атаки персонажа
 */
class AttackSpeed(val person: Person) : Property("Скорость атаки"), IntStatBattle {
    override fun change(newValue: Number) {
        var settingValue = newValue.toDouble()
        if (!person.personBlobs.enableCONSTmaxAttackSpeed.get() && settingValue < person.personValues.maxAttackSpeed.value) {
            settingValue = person.personValues.maxAttackSpeed.value
            maximumValue = settingValue
        }
        if (person.personBlobs.enableCONSTmaxAttackSpeed.get() && settingValue < person.personValues.CONSTmaxAttackSpeed.value) {
            settingValue = person.personValues.CONSTmaxAttackSpeed.value
            maximumValue = settingValue
        }
        super.change(settingValue)
    }
    override fun initForBattle() {
        maximumValue = get()
        setStock(maximumValue!!)
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
    UNDEFINED,
    ON_START_BATTLE,
    ON_DEAL_DAMAGE,
    ON_TAKE_DAMAGE,
    ON_DIE
}

data class PersonListeners (
    val person: Person,

    val onStartBattle: PersonListener = PersonListener("Начало боя", EnumPersonLifects.ON_START_BATTLE),
    val onDealDamage: PersonListener = PersonListener("Атаковал", EnumPersonLifects.ON_DEAL_DAMAGE),
    val onTakeDamage: PersonListener = PersonListener("Получил урон", EnumPersonLifects.ON_TAKE_DAMAGE),
    val onDie: PersonListener = PersonListener("Умер", EnumPersonLifects.ON_DIE)
)