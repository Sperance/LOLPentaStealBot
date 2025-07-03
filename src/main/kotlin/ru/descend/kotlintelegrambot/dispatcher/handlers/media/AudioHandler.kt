package com.github.kotlintelegrambot.dispatcher.handlers.media

import com.github.kotlintelegrambot.dispatcher.handlers.HandleAudio
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.entities.files.Audio

class AudioHandler(
    handleAudio: HandleAudio,
) : MediaHandler<Audio>(
    handleAudio,
    AudioHandlerFunctions::mapMessageToAudio,
    AudioHandlerFunctions::isUpdateAudio,
)

private object AudioHandlerFunctions {

    fun mapMessageToAudio(message: Message): Audio {
        val audio = message.audio
        checkNotNull(audio)
        return audio
    }

    fun isUpdateAudio(update: Update): Boolean = update.message?.audio != null
}
