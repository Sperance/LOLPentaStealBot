package ru.descend.bot.minigame

interface IntStatBattle {
    fun initForBattle()
}

/**
 * Здоровье персонажа
 */
class Health(val person: Person) : Property(EnumPropName.HEALTH), IntStatBattle {
    init {
        onChangeListeners.addListener {
            if (get() <= minimumValue) {
                person.personBlobs.isAlive.set(false)
                person.listeners.onDie.invokeEach(null, null)
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
class Attack(val person: Person) : Property(EnumPropName.ATTACK), IntStatBattle {
    override fun initForBattle() {

    }
}

/**
 * Скорость атаки персонажа
 */
class AttackSpeed(val person: Person) : Property(EnumPropName.ATTACK_SPEED), IntStatBattle {
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
    fun addForItems(stat: BaseProperty) {
        PersonStats::class.java.declaredFields.forEach {
            it.isAccessible = true
            val itField = it.get(this)
            if (itField is Property && itField.name == stat.name) {
                itField.addForItem(stat)
            }
        }
    }
    fun initForBattle() {
        health.initForBattle()
        attack.initForBattle()
        attackSpeed.initForBattle()
    }
}

enum class EnumPersonLifects(val categoryName: String) {
    ON_START_BATTLE("Начало боя"),
    ON_BEFORE_DAMAGE("Перед нанесением урона"),
    ON_DEAL_DAMAGE("При нанесении урона"),
    ON_TAKE_DAMAGE("При получении урона"),
    ON_DIE("При смерти")
}

open class PersonListeners(val person: Person) {
    val onStartBattle: PersonListener = PersonListener(EnumPersonLifects.ON_START_BATTLE)
    val onBeforeDamage: PersonListener = PersonListener(EnumPersonLifects.ON_BEFORE_DAMAGE)
    val onDealDamage: PersonListener = PersonListener(EnumPersonLifects.ON_DEAL_DAMAGE)
    val onTakeDamage: PersonListener = PersonListener(EnumPersonLifects.ON_TAKE_DAMAGE)
    val onDie: PersonListener = PersonListener(EnumPersonLifects.ON_DIE)

    inner class PersonListener(val category: EnumPersonLifects) {
        private val arrayListeners = ArrayList<(Person, Person?, BattleObject?) -> Unit>()
        fun addListener(body: (Person, Person?, BattleObject?) -> Unit) {
            arrayListeners.add(body)
        }
        fun invokeEach(enemy: Person?, battleObj: BattleObject?) {
            arrayListeners.forEach { it.invoke(person, enemy, battleObj) }
        }
    }
}