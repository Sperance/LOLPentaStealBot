package com.github.kotlintelegrambot.dispatcher.handlers

import ru.descend.kotlintelegrambot.Bot
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.entities.User

data class NewChatMembersHandlerEnvironment(
    val bot: Bot,
    val update: Update,
    val message: Message,
    val newChatMembers: List<User>,
)

class NewChatMembersHandler(
    private val handleNewChatMembers: HandleNewChatMembers,
) : Handler {

    override fun checkUpdate(update: Update): Boolean {
        val newChatMembers = update.message?.newChatMembers
        return newChatMembers != null && newChatMembers.isNotEmpty()
    }

    override suspend fun handleUpdate(bot: Bot, update: Update) {
        val message = update.message
        val newChatMembers = message?.newChatMembers
        checkNotNull(newChatMembers)

        val newChatMembersHandlerEnv = NewChatMembersHandlerEnvironment(
            bot,
            update,
            message,
            newChatMembers,
        )
        handleNewChatMembers.invoke(newChatMembersHandlerEnv)
    }
}
