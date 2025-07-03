package com.github.kotlintelegrambot.dispatcher.handlers.media

import com.github.kotlintelegrambot.dispatcher.handlers.HandleVideo
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.entities.files.Video

class VideoHandler(
    handleVideo: HandleVideo,
) : MediaHandler<Video>(
    handleVideo,
    VideoHandlerFunctions::mapMessageToVideo,
    VideoHandlerFunctions::isUpdateVideo,
)

private object VideoHandlerFunctions {

    fun mapMessageToVideo(message: Message): Video {
        val video = message.video
        checkNotNull(video)
        return video
    }

    fun isUpdateVideo(update: Update): Boolean = update.message?.video != null
}
