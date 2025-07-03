package ru.descend.kotlintelegrambot.entities.polls

import ru.descend.kotlintelegrambot.entities.User
import com.google.gson.annotations.SerializedName
import ru.descend.kotlintelegrambot.entities.polls.PollFields

/**
 * Represents an answer of a user in a non-anonymous poll.
 * https://core.telegram.org/bots/api#poll_answer
 */
data class PollAnswer(
    @SerializedName(PollFields.POLL_ID) val pollId: String,
    @SerializedName(PollFields.USER) val user: User,
    @SerializedName(PollFields.OPTION_IDS) val optionIds: List<Int>,
)
