package ru.descend.bot.savedObj

inline fun <reified T> calculateUpdate(before: T, after: T) : String {
    var result = ""
    T::class.java.declaredFields.forEach {
        it.isAccessible = true
        val fieldbefore = it.get(before)
        val fieldafter = it.get(after)
        if (fieldbefore != fieldafter) {
            result += "${it.name} '${fieldbefore}' -> '${fieldafter}'; "
        }
    }
    return result
}