package com.github.kotlintelegrambot.dispatcher.handlers

import ru.descend.kotlintelegrambot.Bot
import ru.descend.kotlintelegrambot.entities.ChatJoinRequest
import ru.descend.kotlintelegrambot.entities.Update

data class NewChatJoinRequestHandlerEnvironment(
    val bot: Bot,
    val update: Update,
    val chatJoinRequest: ChatJoinRequest,
)

class NewChatJoinRequestHandler(
    private val handleNewChatJoinRequest: HandleNewChatJoinRequest,
) : Handler {
    override fun checkUpdate(update: Update): Boolean {
        return update.chatJoinRequest != null
    }

    override suspend fun handleUpdate(bot: Bot, update: Update) {
        checkNotNull(update.chatJoinRequest)

        val newChatJoinRequestHandlerEnv = NewChatJoinRequestHandlerEnvironment(
            bot,
            update,
            update.chatJoinRequest,
        )
        handleNewChatJoinRequest(newChatJoinRequestHandlerEnv)
    }
}
