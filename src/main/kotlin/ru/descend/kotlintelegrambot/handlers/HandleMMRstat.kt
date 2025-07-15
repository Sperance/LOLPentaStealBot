package ru.descend.kotlintelegrambot.handlers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.descend.bot.printLog
import ru.descend.kotlintelegrambot.dispatcher.Dispatcher
import ru.descend.kotlintelegrambot.dispatcher.command
import ru.descend.kotlintelegrambot.entities.ChatId
import java.util.Date
import kotlin.time.Duration.Companion.minutes

private var global_listening_counter = 0
private var global_listeting_chat = -1L
private var listeningJob: Job? = null
val listening_data_array = ArrayList<String>()
var last_date_loaded_matches: Date? = null

fun Dispatcher.handleMMRstat() {
    command("listening_test") {
        listening_data_array.add("TestStr ${System.currentTimeMillis()}")
    }
    command("listening_status") {
        bot.sendMessage(ChatId.fromId(message.chat.id), "Статус: $global_listening_counter. Job: ${listeningJob?.isActive}. Last loaded: $last_date_loaded_matches")
    }
    command("listening_start") {
        if (global_listening_counter != 0) {
            bot.sendMessage(ChatId.fromId(message.chat.id), "Слушатель матчей уже запущен (чат $global_listeting_chat). Повторный запуск отменён")
        } else {
            global_listening_counter++

            global_listeting_chat = message.chat.id
            bot.sendMessage(ChatId.fromId(global_listeting_chat), "Слушатель матчей запущен. Ожидание новых матчей...")

            listeningJob = CoroutineScope(Dispatchers.IO).launch {
                while (isActive && global_listening_counter > 0) {
                    if (listening_data_array.isNotEmpty()) {
                        var stringData = ""
                        listening_data_array.forEach { str ->
                            stringData += str
                        }
                        listening_data_array.clear()
                        bot.sendMessage(ChatId.fromId(message.chat.id), stringData).fold(
                            { t -> printLog("SECCESS SENDED message ${t.messageId}") },
                            { t -> printLog("ERROR SENDED message: $t") }
                        )
                    }
                    delay((1).minutes)
                }
            }
        }
    }
    command("listening_stop") {
        if (global_listening_counter > 0) {
            global_listening_counter--
            listeningJob?.cancel()
            global_listeting_chat = -1L
            last_date_loaded_matches = null
            bot.sendMessage(ChatId.fromId(message.chat.id), "Слушатель матчей успешно завершен")
        } else {
            bot.sendMessage(ChatId.fromId(message.chat.id), "Слушатель матчей не запущен. Нечего завершать")
        }
    }
}