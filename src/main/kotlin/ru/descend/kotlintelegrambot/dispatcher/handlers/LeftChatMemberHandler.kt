package com.github.kotlintelegrambot.dispatcher.handlers

import ru.descend.kotlintelegrambot.Bot
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.entities.User

data class LeftChatMemberHandlerEnvironment(
    val bot: Bot,
    val update: Update,
    val message: Message,
    val leftChatMember: User,
)

class LeftChatMemberHandler(
    private val handleLeftChatMember: HandleLeftChatMember,
) : Handler {

    override fun checkUpdate(update: Update): Boolean = update.message?.leftChatMember != null

    override suspend fun handleUpdate(bot: Bot, update: Update) {
        val message = update.message
        val leftChatMember = message?.leftChatMember
        checkNotNull(leftChatMember)

        handleLeftChatMember.invoke(
            LeftChatMemberHandlerEnvironment(
                bot,
                update,
                message,
                leftChatMember,
            ),
        )
    }
}
