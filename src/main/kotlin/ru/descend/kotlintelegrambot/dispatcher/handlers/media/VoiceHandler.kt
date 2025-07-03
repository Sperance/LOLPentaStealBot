package com.github.kotlintelegrambot.dispatcher.handlers.media

import com.github.kotlintelegrambot.dispatcher.handlers.HandleVoice
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.entities.files.Voice

class VoiceHandler(
    handleVoice: HandleVoice,
) : MediaHandler<Voice>(
    handleVoice,
    VoiceHandlerFunctions::mapMessageToVoice,
    VoiceHandlerFunctions::isUpdateVoice,
)

private object VoiceHandlerFunctions {

    fun mapMessageToVoice(message: Message): Voice {
        val voice = message.voice
        checkNotNull(voice)
        return voice
    }

    fun isUpdateVoice(update: Update): Boolean = update.message?.voice != null
}
