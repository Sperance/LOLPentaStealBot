package com.github.kotlintelegrambot.dispatcher.handlers

import ru.descend.kotlintelegrambot.Bot
import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.entities.polls.PollAnswer

data class PollAnswerHandlerEnvironment(
    val bot: Bot,
    val update: Update,
    val pollAnswer: PollAnswer,
)

class PollAnswerHandler(
    private val handlePollAnswer: HandlePollAnswer,
) : Handler {

    override fun checkUpdate(update: Update): Boolean = update.pollAnswer != null

    override suspend fun handleUpdate(bot: Bot, update: Update) {
        val pollAnswer = update.pollAnswer
        checkNotNull(pollAnswer)

        val pollAnswerHandlerEnv = PollAnswerHandlerEnvironment(bot, update, pollAnswer)
        handlePollAnswer(pollAnswerHandlerEnv)
    }
}
