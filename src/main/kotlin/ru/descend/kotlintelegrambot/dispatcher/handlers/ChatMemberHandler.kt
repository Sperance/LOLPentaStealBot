package com.github.kotlintelegrambot.dispatcher.handlers

import ru.descend.kotlintelegrambot.Bot
import ru.descend.kotlintelegrambot.entities.ChatMemberUpdated
import ru.descend.kotlintelegrambot.entities.Update

data class ChatMemberHandlerEnvironment(
    val bot: Bot,
    val update: Update,
    val chatMember: ChatMemberUpdated,
)

class ChatMemberHandler(
    private val handleChatMember: HandleChatMember,
) : Handler {
    override fun checkUpdate(update: Update): Boolean = update.chatMember != null
    override suspend fun handleUpdate(bot: Bot, update: Update) {
        checkNotNull(update.chatMember)
        handleChatMember(ChatMemberHandlerEnvironment(bot, update, update.chatMember))
    }
}
