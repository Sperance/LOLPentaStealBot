package ru.descend.bot.savedObj

import ru.descend.bot.toDate
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun Date.toLocalDate(): LocalDate = LocalDate.parse(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(getDate(this.time)))
fun Long.toLocalDate(): LocalDate = LocalDate.parse(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(getDate(this)))

fun Long?.toDate(): Date {
    return Date(this?:Date().time)
}

fun Date.isCurrentDay() : Boolean {
    val currentDate = System.currentTimeMillis().toDate().getStrongDate()
    val inDate = this.getStrongDate()
    return currentDate.date == inDate.date
}

fun LocalDate.toDate(): Date = Date.from(
    this
        .atStartOfDay(
            ZoneId.systemDefault()
        )
        .toInstant()
)

fun Date.getStringTime(withSeconds: Boolean = false): String {
    return if (withSeconds)
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(this)
    else
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(this)
}

fun getDate(day: Int, month: Int, year: Int): Date =
    Calendar.getInstance().apply {
        set(Calendar.DAY_OF_YEAR, day)
        set(Calendar.MONTH, month)
        set(Calendar.YEAR, year)
    }.time

fun getDate(time: Long?): Date {

    if (time == null)
        return Date()

    val settedTime = Calendar.getInstance()
    settedTime.time = Date(time)
    return getDate(settedTime.get(Calendar.DAY_OF_YEAR), settedTime.get(Calendar.MONTH), settedTime.get(Calendar.YEAR))
}

/**
 * Преобразование подаваемой даты в удобный для форматирования объект
 */
fun Date.getStrongDate() : StrongDate{
    return getStrongDate(time)
}

/**
 * Преобразование подаваемой даты в удобный для форматирования объект
 */
fun getStrongDate(date: Long?): StrongDate {

    if (date == null)
        return getStrongDate(Date().time)

    val calendar = Calendar.getInstance().apply { time = Date(date) }

    val dayOfWeekName:String = when (calendar.get(Calendar.DAY_OF_WEEK)){
        Calendar.MONDAY -> "понедельник"
        Calendar.TUESDAY -> "вторник"
        Calendar.WEDNESDAY -> "среда"
        Calendar.THURSDAY -> "четверг"
        Calendar.FRIDAY -> "пятница"
        Calendar.SATURDAY -> "суббота"
        Calendar.SUNDAY -> "воскресенье"
        else -> ""
    }

    val dayOfWeekNameSimple:String = when (calendar.get(Calendar.DAY_OF_WEEK)){
        Calendar.MONDAY -> "пн."
        Calendar.TUESDAY -> "вт."
        Calendar.WEDNESDAY -> "ср."
        Calendar.THURSDAY -> "чт."
        Calendar.FRIDAY -> "пт."
        Calendar.SATURDAY -> "сб."
        Calendar.SUNDAY -> "вс."
        else -> {""}
    }

    val dayName = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH)

    val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
    val timeSSText = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(calendar.time)
    val dateText = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)
    val year = calendar.get(Calendar.YEAR)

    val hours = calendar.get(Calendar.HOUR_OF_DAY).toLong()
    val minute = calendar.get(Calendar.MINUTE).toLong()
    val second = calendar.get(Calendar.SECOND).toLong()
    val milliseconds = calendar.get(Calendar.MILLISECOND).toLong()

    return StrongDate(dayOfWeekName = dayOfWeekName, dayOfWeekNameSimple = dayOfWeekNameSimple, dayOfWeek = dayName, time = timeText, timeSec = timeSSText, date = dateText, timeLong = calendar.timeInMillis, year = year, dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH), month = month, seconds = second, milliseconds = milliseconds, minutes = minute, hours = hours)
}

/**
 * Класс для преобразования даты
 */
data class StrongDate(
    /**
     * Наименование для недели
     */
    var dayOfWeekName: String,
    /**
     * Наименование для недели (в коротком виде)
     */
    var dayOfWeekNameSimple: String,
    /**
     * День недели
     */
    var dayOfWeek: Int,
    /**
     * Время HH:mm
     */
    var time: String,
    /**
     * Время HH:mm:ss
     */
    var timeSec: String,
    /**
     * Дата dd-MM-yyyy
     */
    var date: String,
    /**
     * Дата в миллисекундах
     */
    var timeLong: Long,
    /**
     * Часы
     */
    var hours: Long,
    /**
     * Минуты
     */
    var minutes: Long,
    /**
     * Секунды
     */
    var seconds: Long,
    /**
     * Миллисекунды
     */
    var milliseconds: Long,
    /**
     * День в месяце
     */
    var dayOfMonth: Int,
    /**
     * Номер месяца
     */
    var month: Int,
    /**
     * Год
     */
    var year: Int
)