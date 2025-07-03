package ru.descend.kotlintelegrambot.handlers

import ru.descend.kotlintelegrambot.dispatcher.Dispatcher
import ru.descend.kotlintelegrambot.dispatcher.dice
import ru.descend.kotlintelegrambot.entities.ChatId

fun Dispatcher.handleOthers() {
    dice {
        bot.sendMessage(ChatId.fromId(message.chat.id), "A dice ${dice.emoji.emojiValue} with value ${dice.value} has been received!")
    }
}
