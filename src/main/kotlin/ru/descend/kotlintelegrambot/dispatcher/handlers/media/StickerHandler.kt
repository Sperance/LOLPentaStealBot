package com.github.kotlintelegrambot.dispatcher.handlers.media

import com.github.kotlintelegrambot.dispatcher.handlers.HandleSticker
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.entities.stickers.Sticker

class StickerHandler(
    handleSticker: HandleSticker,
) : MediaHandler<Sticker>(
    handleSticker,
    StickerHandlerFunctions::mapMessageToSticker,
    StickerHandlerFunctions::isUpdateSticker,
)

private object StickerHandlerFunctions {

    fun mapMessageToSticker(message: Message): Sticker {
        val sticker = message.sticker
        checkNotNull(sticker)
        return sticker
    }

    fun isUpdateSticker(update: Update): Boolean = update.message?.sticker != null
}
