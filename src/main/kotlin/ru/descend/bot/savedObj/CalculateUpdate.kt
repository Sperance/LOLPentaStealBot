package ru.descend.bot.savedObj

inline fun <reified T> calculateUpdate(before: T?, after: T?) : String {
    if (before == null) return ""
    if (after == null) return ""
    var result = ""
    T::class.java.declaredFields.forEach {
        it.isAccessible = true
        val fieldbefore = it.get(before)
        val fieldafter = it.get(after)
        if (fieldbefore != fieldafter && it.name != "updatedAt" && it.name != "createdAt") {
            result += "${it.name} '${fieldbefore}' -> '${fieldafter}'; "
        }
    }
    return result
}