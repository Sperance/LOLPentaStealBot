package ru.descend.kotlintelegrambot

import ru.descend.kotlintelegrambot.entities.Update
import com.google.gson.Gson

internal class UpdateMapper(private val gson: Gson) {

    fun jsonToUpdate(updateJson: String): Update = gson.fromJson(updateJson, Update::class.java)
}
