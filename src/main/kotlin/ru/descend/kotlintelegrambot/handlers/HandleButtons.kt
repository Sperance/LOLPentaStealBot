package ru.descend.kotlintelegrambot.handlers

import ru.descend.kotlintelegrambot.dispatcher.Dispatcher
import ru.descend.kotlintelegrambot.dispatcher.callbackQuery
import ru.descend.kotlintelegrambot.dispatcher.command
import ru.descend.kotlintelegrambot.entities.ChatId
import ru.descend.kotlintelegrambot.entities.InlineKeyboardMarkup
import ru.descend.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

fun Dispatcher.handleButtons() {
    command("buttons") {
        val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
            listOf(InlineKeyboardButton.CallbackData(text = "Просто нажать", callbackData = "simpleClick"), ),
            listOf(InlineKeyboardButton.CallbackData(text = "Показать сообщение", callbackData = "simpleMessage")),
        )
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "Выберите одну из кнопок:",
            replyMarkup = inlineKeyboardMarkup,
        )
    }

    callbackQuery("simpleClick") {
        println("[${callbackQuery.from.firstName}::${callbackQuery.from.username}](${callbackQuery.from.id}) Clicked: ${callbackQuery.data}")
        //val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
        //bot.sendMessage(ChatId.fromId(chatId), callbackQuery.data)
    }

    callbackQuery(callbackData = "simpleMessage", callbackAnswerText = "Сообщение успешно показано", callbackAnswerShowAlert = true) {
        println("[${callbackQuery.from.firstName}::${callbackQuery.from.username}](${callbackQuery.from.id}) Clicked: ${callbackQuery.data}")
        //val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
        //bot.sendMessage(ChatId.fromId(chatId), callbackQuery.data)
    }
}
