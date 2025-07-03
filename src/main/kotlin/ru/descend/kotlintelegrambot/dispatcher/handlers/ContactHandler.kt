package com.github.kotlintelegrambot.dispatcher.handlers

import ru.descend.kotlintelegrambot.Bot
import ru.descend.kotlintelegrambot.entities.Contact
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update

data class ContactHandlerEnvironment(
    val bot: Bot,
    val update: Update,
    val message: Message,
    val contact: Contact,
)

class ContactHandler(
    private val handleContact: HandleContact,
) : Handler {

    override fun checkUpdate(update: Update): Boolean {
        return update.message?.contact != null
    }

    override suspend fun handleUpdate(bot: Bot, update: Update) {
        checkNotNull(update.message)
        checkNotNull(update.message.contact)

        val contactHandlerEnv = ContactHandlerEnvironment(
            bot,
            update,
            update.message,
            update.message.contact,
        )
        handleContact(contactHandlerEnv)
    }
}
