package ru.descend.kotlintelegrambot.samples

import ru.descend.kotlintelegrambot.bot
import ru.descend.kotlintelegrambot.dispatch
import ru.descend.kotlintelegrambot.dispatcher.text
import ru.descend.kotlintelegrambot.entities.ChatId

fun main() {
    val bot = bot {
        dispatch {
            text {
                println("Text: $text Author: ${this.message.from}")
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    messageThreadId = message.messageThreadId,
                    text = text + "123123",
                    protectContent = true,
                    disableNotification = false,
                )
            }
        }
    }

    bot.startPolling()
}
