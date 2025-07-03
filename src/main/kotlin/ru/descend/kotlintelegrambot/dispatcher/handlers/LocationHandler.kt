package com.github.kotlintelegrambot.dispatcher.handlers

import ru.descend.kotlintelegrambot.Bot
import ru.descend.kotlintelegrambot.entities.Location
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update

data class LocationHandlerEnvironment(
    val bot: Bot,
    val update: Update,
    val message: Message,
    val location: Location,
)

class LocationHandler(
    private val handleLocation: HandleLocation,
) : Handler {

    override fun checkUpdate(update: Update): Boolean {
        return update.message?.location != null
    }

    override suspend fun handleUpdate(bot: Bot, update: Update) {
        checkNotNull(update.message)
        checkNotNull(update.message.location)

        val locationHandlerEnv = LocationHandlerEnvironment(
            bot,
            update,
            update.message,
            update.message.location,
        )
        handleLocation(locationHandlerEnv)
    }
}
