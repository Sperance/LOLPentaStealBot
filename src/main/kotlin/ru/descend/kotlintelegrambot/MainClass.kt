package ru.descend.kotlintelegrambot

import ru.descend.kotlintelegrambot.dispatcher.telegramError
import ru.descend.kotlintelegrambot.handlers.handleButtons
import ru.descend.kotlintelegrambot.handlers.handleCommands
import ru.descend.kotlintelegrambot.handlers.handleOthers

fun main() {
    val bot = bot {
        timeout = 30
        dispatch {
            handleButtons()
            handleCommands()
            handleOthers()

            telegramError {
                println(error.getErrorMessage())
            }
        }
    }
    bot.startPolling()
}
