package com.github.kotlintelegrambot.dispatcher.handlers.media

import com.github.kotlintelegrambot.dispatcher.handlers.HandleVideoNote
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.entities.files.VideoNote

class VideoNoteHandler(
    handleVideoNote: HandleVideoNote,
) : MediaHandler<VideoNote>(
    handleVideoNote,
    VideoNoteHandlerFunctions::mapMessageToVideoNote,
    VideoNoteHandlerFunctions::isUpdateVideoNote,
)

private object VideoNoteHandlerFunctions {

    fun mapMessageToVideoNote(message: Message): VideoNote {
        val videoNote = message.videoNote
        checkNotNull(videoNote)
        return videoNote
    }

    fun isUpdateVideoNote(update: Update): Boolean = update.message?.videoNote != null
}
