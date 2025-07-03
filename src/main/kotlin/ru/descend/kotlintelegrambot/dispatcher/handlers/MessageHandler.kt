package com.github.kotlintelegrambot.dispatcher.handlers

import ru.descend.kotlintelegrambot.Bot
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.extensions.filters.Filter

data class MessageHandlerEnvironment(
    val bot: Bot,
    val update: Update,
    val message: Message,
)

class MessageHandler(
    private val filter: Filter,
    private val handleMessage: suspend MessageHandlerEnvironment.() -> Unit,
) : Handler {

    override fun checkUpdate(update: Update): Boolean =
        if (update.message == null) {
            false
        } else {
            filter.checkFor(update.message)
        }

    override suspend fun handleUpdate(bot: Bot, update: Update) {
        checkNotNull(update.message)
        val messageHandlerEnv = MessageHandlerEnvironment(bot, update, update.message)
        handleMessage(messageHandlerEnv)
    }
}
