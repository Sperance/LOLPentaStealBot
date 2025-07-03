package com.github.kotlintelegrambot.dispatcher.handlers.media

import ru.descend.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update

data class MediaHandlerEnvironment<Media>(
    val bot: Bot,
    val update: Update,
    val message: Message,
    val media: Media,
)

abstract class MediaHandler<Media>(
    private val handleMediaUpdate: suspend MediaHandlerEnvironment<Media>.() -> Unit,
    private val toMedia: Message.() -> Media,
    private val isUpdateMedia: (Update) -> Boolean,
) : Handler {

    override fun checkUpdate(update: Update): Boolean = isUpdateMedia(update)

    override suspend fun handleUpdate(bot: Bot, update: Update) {
        checkNotNull(update.message)
        val media = update.message.toMedia()
        val mediaHandlerEnvironment = MediaHandlerEnvironment(bot, update, update.message, media)
        handleMediaUpdate.invoke(mediaHandlerEnvironment)
    }
}
