package ru.descend.kotlintelegrambot.handlers

import ru.descend.kotlintelegrambot.dispatcher.Dispatcher
import ru.descend.kotlintelegrambot.dispatcher.command
import ru.descend.kotlintelegrambot.entities.ChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Dispatcher.handleCommands() {
    command("checkWork") {
        val result = bot.sendMessage(chatId = ChatId.fromId(update.message!!.chat.id), text = "Bot is working")
        result.fold(
            { println("response") },
            { println("error") },
        )
    }
    command("startListen") {
        CoroutineScope(Dispatchers.IO).launch {
            var str = 0
            while (true) {
                str++
                val result = bot.sendMessage(chatId = ChatId.fromId(update.message!!.chat.id), text = "STARTED $str")
                result.fold(
                    { println("response") },
                    { println("error") },
                )
                delay(1000)
            }
        }
    }
}
