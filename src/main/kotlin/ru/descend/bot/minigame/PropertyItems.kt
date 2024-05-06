package ru.descend.bot.minigame

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.descend.bot.minigame.gamedatas.CalculateBattle
import ru.descend.bot.postgre.r2dbc.getField
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.printLog
import ru.descend.bot.to2Digits
import java.time.LocalDateTime
import java.util.TimerTask
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


enum class EnumItemCategory(nameCategory: String) {
    COMMON("Обычный"),
    UNCOMMON("Необычный"),
    RARE("Редкий")
}

data class ItemsConstants(
    var isSellable: Boolean = true,
    var isSellableOthers: Boolean = true,
    var isQuests: Boolean = true,
)

data class ObjectsConstants(
    var isEquipped: Boolean = false
)

data class PersonConstants(
    var isDie: Boolean = false
)

open class BaseItem (
    val name: String,
    var constItem: ItemsConstants = ItemsConstants(),
    val constObject: ObjectsConstants = ObjectsConstants(),
    val calcProps: PropertiesCalculableItem = PropertiesCalculableItem(),
    val props: PropertiesItem = PropertiesItem(),
    var category: EnumItemCategory = EnumItemCategory.COMMON
)

open class BasePerson (
    val name: String,
    val calcProps: PropertiesCalculablePerson = PropertiesCalculablePerson(),
    val props: PropertiesPerson = PropertiesPerson(),
    val constants: PersonConstants = PersonConstants()
) {
    fun isDie() = constants.isDie

    fun initForBattle() {
        calcProps.initForBattle()
    }

    fun onAttack(enemy: BasePerson) {
        val currentDamage = calcProps.attackDamage.get().to2Digits()
        var enemyHealth = enemy.calcProps.health.getForBattle().to2Digits()
        enemyHealth -= currentDamage
        if (enemyHealth <= 0.0) {
            enemyHealth = 0.0
            enemy.constants.isDie = true
        }
        enemy.calcProps.health.setForBattle(enemyHealth.to2Digits())
    }
}

class ObjectItem (
    name: String,
) : BaseItem(name)

class Person(
    name: String,
    private var arrayItems: ArrayList<BaseItem> = ArrayList(),
) : BasePerson(name) {

    fun calcProperties() : PropertiesCalculablePerson {
        val props = calcProps.copy()
        arrayItems.forEach {
            if (it is ObjectItem) {
                PropertiesCalculablePerson::class.java.declaredFields.forEach { fld ->
                    val getting = props.getField(fld.name)
                    if (getting is PropertyStatGlobal) {
                        getting.add(it.calcProps.getField(fld.name) as PropertyStatLocal)
                    } else if (getting is PropertyValue) {
                        getting.add(it.calcProps.getField(fld.name) as PropertyValue)
                    }
                }
            }
        }
        return props
    }
}

class TestClass {
    @Test
    fun test_check() {
        val person = Person("Человек Павук")
        person.calcProps.health.value = 100.0
        person.calcProps.attackSpeed.value = 2500.0
        person.calcProps.attackDamage.value = 15.0

        val enemy = BasePerson("Дух волка")
        enemy.calcProps.health.value = 10.0
        enemy.calcProps.attackSpeed.value = 700.0
        enemy.calcProps.attackDamage.value = 7.3

        runBlocking {
            val battle = CalculateBattle(person, enemy)
            battle.doBattle()
        }
    }

    @Test
    fun test_check_sa() {
        val arrayKORDmmr = ArrayList<Pair<Int?, Double>>()
        arrayKORDmmr.add(Pair(null, 5.0))
        arrayKORDmmr.add(Pair(1, 4.0))
        arrayKORDmmr.add(Pair(2, 6.0))
        arrayKORDmmr.add(Pair(3, 5.0))
        arrayKORDmmr.add(Pair(4, 7.0))
        arrayKORDmmr.add(Pair(null, 2.0))

        printLog(arrayKORDmmr.maxBy { it.second })
        printLog(arrayKORDmmr.minBy { it.second })
    }

//    @Test
//    fun test_gson() {
//
//        val gson = GsonBuilder()
//            .registerTypeAdapter(Property::class.java, TypeAdapterProperty())
//            .registerTypeAdapter(PropertyValue::class.java, TypeAdapterPropertyValue())
//            .enableComplexMapKeySerialization()
//            .serializeNulls()
//            .setDateFormat(DateFormat.LONG)
//            .setPrettyPrinting()
//            .create()

//        val data = DataProperties(false)
//        data.strength.value = 5.0
//        data.level.value = 3.0
//
//        printLog("Object before: $data")
//        val text = gson.toJson(data)
//        printLog("Result: $text")
//        printLog("Object after: ${gson.fromJson(text, DataProperties::class.java)}")
//    }
//}

//class TypeAdapterProperty : TypeAdapter<Property>() {
//    private val stringChar = "|"
//    override fun write(out: JsonWriter, value: Property) {
//        val valueString = value.name +
//                stringChar + value.gsonClassCode +
//                stringChar + value.gsonCategoryCode +
//                stringChar + value.value
//        out.value(valueString)
//    }
//    override fun read(`in`: JsonReader): Property {
//        val valueStringObject = `in`.nextString()
//        val splittedObject = valueStringObject.split(stringChar)
//        val name = splittedObject[0]
//        val gsonClassCode = splittedObject[1].toInt()
//        val gsonCategoryCode = splittedObject[2].toInt()
//        val value = splittedObject[3].toDouble()
//        return Property(name, gsonClassCode, gsonCategoryCode, value)
//    }
//}
//
//class TypeAdapterPropertyValue : TypeAdapter<PropertyValue>() {
//    private val stringChar = "|"
//    override fun write(out: JsonWriter, value: PropertyValue) {
//        val valueString = value.name +
//                stringChar + value.gsonCategoryCode +
//                stringChar + value.isUnit +
//                stringChar + value.value +
//                stringChar + value.localPercent +
//                stringChar + value.globalPercent
//        out.value(valueString)
//    }
//    override fun read(`in`: JsonReader): PropertyValue {
//        val valueStringObject = `in`.nextString()
//        val splittedObject = valueStringObject.split(stringChar)
//        val name = splittedObject[0]
//        val gsonCategoryCode = splittedObject[1].toInt()
//        val isUnit = splittedObject[2].toBoolean()
//        val value = splittedObject[3].toDouble()
//        val localPercent = splittedObject[4].toDouble()
//        val globalPercent = splittedObject[5].toDouble()
//        val returnedValue = PropertyValue(name, gsonCategoryCode, isUnit, localPercent = localPercent, globalPercent = globalPercent)
//        returnedValue.value = value
//        return returnedValue
//    }
}