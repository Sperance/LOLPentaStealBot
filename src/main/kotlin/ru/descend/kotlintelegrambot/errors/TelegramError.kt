package ru.descend.kotlintelegrambot.errors

import ru.descend.kotlintelegrambot.types.DispatchableObject

interface TelegramError : DispatchableObject {
    enum class Error {
        RETRIEVE_UPDATES,
    }

    fun getType(): Error
    fun getErrorMessage(): String
}
