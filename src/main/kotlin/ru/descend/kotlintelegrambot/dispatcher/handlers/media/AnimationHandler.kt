package com.github.kotlintelegrambot.dispatcher.handlers.media

import com.github.kotlintelegrambot.dispatcher.handlers.HandleAnimation
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.entities.files.Animation

class AnimationHandler(
    handleAnimation: HandleAnimation,
) : MediaHandler<Animation>(
    handleAnimation,
    AnimationHandlerFunctions::mapMessageToAnimation,
    AnimationHandlerFunctions::updateIsAnimation,
)

private object AnimationHandlerFunctions {

    fun mapMessageToAnimation(message: Message): Animation {
        checkNotNull(message.animation)
        return message.animation
    }

    fun updateIsAnimation(update: Update): Boolean = update.message?.animation != null
}
