package ru.descend.kotlintelegrambot.samples

import ru.descend.kotlintelegrambot.bot
import ru.descend.kotlintelegrambot.dispatch
import ru.descend.kotlintelegrambot.dispatcher.command
import ru.descend.kotlintelegrambot.dispatcher.pollAnswer
import ru.descend.kotlintelegrambot.entities.ChatId
import ru.descend.kotlintelegrambot.entities.polls.PollType.QUIZ

fun main() {
    bot {
        dispatch {
            pollAnswer {
                println("${pollAnswer.user.username} has selected the option ${pollAnswer.optionIds.lastOrNull()} in the poll ${pollAnswer.pollId}")
            }
            command("regularPoll") {
                bot.sendPoll(
                    chatId = ChatId.fromId(message.chat.id),
                    question = "Пойдём ли завтра кушать Плов?",
                    options = listOf("Да", "Точно пойдём", "Позже, но пойдём"),
                    isAnonymous = false,
                )
            }

            command("quizPoll") {
                bot.sendPoll(
                    chatId = ChatId.fromId(message.chat.id),
                    type = QUIZ,
                    question = "Java or Kotlin?",
                    options = listOf("Java", "Kotlin"),
                    correctOptionId = 1,
                    isAnonymous = false,
                )
            }

            command("closedPoll") {
                bot.sendPoll(
                    chatId = ChatId.fromId(message.chat.id),
                    type = QUIZ,
                    question = "Foo or Bar?",
                    options = listOf("Foo", "Bar", "FooBar"),
                    correctOptionId = 1,
                    isClosed = false,
                    explanation = "A closed quiz because I can",
                )
            }
        }
    }.startPolling()
}
