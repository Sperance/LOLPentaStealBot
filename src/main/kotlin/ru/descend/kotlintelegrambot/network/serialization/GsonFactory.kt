package ru.descend.kotlintelegrambot.network.serialization

import ru.descend.kotlintelegrambot.entities.TelegramFile
import ru.descend.kotlintelegrambot.entities.dice.DiceEmoji
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult
import ru.descend.kotlintelegrambot.entities.inputmedia.GroupableMedia
import ru.descend.kotlintelegrambot.entities.inputmedia.InputMedia
import ru.descend.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import ru.descend.kotlintelegrambot.entities.reaction.ReactionType
import ru.descend.kotlintelegrambot.network.serialization.adapter.DiceEmojiAdapter
import ru.descend.kotlintelegrambot.network.serialization.adapter.GroupableMediaAdapter
import ru.descend.kotlintelegrambot.network.serialization.adapter.InlineKeyboardButtonAdapter
import ru.descend.kotlintelegrambot.network.serialization.adapter.InlineQueryResultAdapter
import ru.descend.kotlintelegrambot.network.serialization.adapter.InputMediaAdapter
import ru.descend.kotlintelegrambot.network.serialization.adapter.ReactionTypeAdapter
import ru.descend.kotlintelegrambot.network.serialization.adapter.TelegramFileAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder

internal object GsonFactory {

    fun createForApiClient(): Gson = GsonBuilder()
        .registerTypeAdapter(InlineQueryResult::class.java, InlineQueryResultAdapter())
        .registerTypeAdapter(InlineKeyboardButton::class.java, InlineKeyboardButtonAdapter())
        .registerTypeAdapter(DiceEmoji::class.java, DiceEmojiAdapter())
        .registerTypeAdapter(TelegramFile.ByFile::class.java, TelegramFileAdapter())
        .registerTypeAdapter(TelegramFile::class.java, TelegramFileAdapter())
        .registerTypeAdapter(InputMedia::class.java, InputMediaAdapter())
        .registerTypeAdapter(GroupableMedia::class.java, GroupableMediaAdapter(InputMediaAdapter()))
        .registerTypeAdapter(ReactionType::class.java, ReactionTypeAdapter())
        .create()

    fun createForMultipartBodyFactory(): Gson = GsonBuilder()
        .registerTypeAdapter(TelegramFile.ByFile::class.java, TelegramFileAdapter())
        .registerTypeAdapter(TelegramFile::class.java, TelegramFileAdapter())
        .registerTypeAdapter(GroupableMedia::class.java, GroupableMediaAdapter(InputMediaAdapter()))
        .create()
}
