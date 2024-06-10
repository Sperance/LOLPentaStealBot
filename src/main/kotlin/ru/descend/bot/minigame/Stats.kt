package ru.descend.bot.minigame

import kotlinx.serialization.Serializable
import ru.descend.bot.listFields

/**
 * Здоровье персонажа
 */
@Serializable
class Health : Property(EnumPropName.HEALTH) {
    override fun initForBattle(person: Person) {
        maximumValue = get()
        setStock(maximumValue!!)

        onChangeListeners.addListener {
            if (it.get() == it.minimumValue) {
                person.personBlobs.isAlive.set(false)
            }
        }
    }
}

/**
 * Атака персонажа
 */
@Serializable
class Attack : Property(EnumPropName.ATTACK) {
    override fun initForBattle(person: Person) {
        maximumValue = get()
        setStock(maximumValue!!)
    }
}

/**
 * Скорость атаки персонажа
 */
@Serializable
class AttackSpeed : Property(EnumPropName.ATTACK_SPEED) {
//    override fun change(newValue: Number) {
//        var settingValue = newValue.toDouble()
//        if (!person.personBlobs.enableCONSTmaxAttackSpeed.get() && settingValue < person.personValues.maxAttackSpeed.value) {
//            settingValue = person.personValues.maxAttackSpeed.value
//            maximumValue = settingValue
//        }
//        if (person.personBlobs.enableCONSTmaxAttackSpeed.get() && settingValue < person.personValues.CONSTmaxAttackSpeed.value) {
//            settingValue = person.personValues.CONSTmaxAttackSpeed.value
//            maximumValue = settingValue
//        }
//        super.change(settingValue)
//    }
    override fun initForBattle(person: Person) {
        minimumValue = person.personValues.CONSTmaxAttackSpeed.value
        maximumValue = get()
        setStock(maximumValue!!)
        println("init stock $maximumValue")

        onChangeListeners.addListener {
            var settingValue = it.get()
            if (!person.personBlobs.enableCONSTmaxAttackSpeed.get() && settingValue < person.personValues.maxAttackSpeed.value) {
                settingValue = person.personValues.maxAttackSpeed.value
                maximumValue = settingValue
                println("limited 1 set $settingValue")
            }
            setStock(settingValue, false)
            println("set stock $settingValue")
        }
    }
}

@Serializable
data class PersonStats (
    val health: Health = Health(),
    val attack: Attack = Attack(),
    val attackSpeed: AttackSpeed = AttackSpeed()
) {
    fun addForItems(stat: BaseProperty) {
        listFields<BaseProperty>().forEach {itField ->
            if (itField is Property && itField.name == stat.name) {
                itField.addForItem(stat)
            }
        }
    }
    fun initForBattle(person: Person) {
        listFields<Property>().forEach {
            it.initForBattle(person)
        }
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