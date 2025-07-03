package ru.descend.kotlintelegrambot.samples

import ru.descend.kotlintelegrambot.bot
import ru.descend.kotlintelegrambot.dispatch
import ru.descend.kotlintelegrambot.dispatcher.callbackQuery
import ru.descend.kotlintelegrambot.dispatcher.channel
import ru.descend.kotlintelegrambot.dispatcher.chosenInlineResult
import ru.descend.kotlintelegrambot.dispatcher.command
import ru.descend.kotlintelegrambot.dispatcher.contact
import ru.descend.kotlintelegrambot.dispatcher.dice
import ru.descend.kotlintelegrambot.dispatcher.inlineQuery
import ru.descend.kotlintelegrambot.dispatcher.location
import ru.descend.kotlintelegrambot.dispatcher.message
import ru.descend.kotlintelegrambot.dispatcher.photos
import ru.descend.kotlintelegrambot.dispatcher.telegramError
import ru.descend.kotlintelegrambot.dispatcher.text
import ru.descend.kotlintelegrambot.entities.ChatId
import ru.descend.kotlintelegrambot.entities.InlineKeyboardMarkup
import ru.descend.kotlintelegrambot.entities.KeyboardReplyMarkup
import ru.descend.kotlintelegrambot.entities.ParseMode.MARKDOWN
import ru.descend.kotlintelegrambot.entities.ParseMode.MARKDOWN_V2
import ru.descend.kotlintelegrambot.entities.ReplyKeyboardRemove
import ru.descend.kotlintelegrambot.entities.TelegramFile.ByUrl
import ru.descend.kotlintelegrambot.entities.dice.DiceEmoji
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InputMessageContent
import ru.descend.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import ru.descend.kotlintelegrambot.entities.inputmedia.MediaGroup
import ru.descend.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import ru.descend.kotlintelegrambot.entities.keyboard.KeyboardButton
import ru.descend.kotlintelegrambot.extensions.filters.Filter

fun main() {
    val bot = bot {
        timeout = 30

        dispatch {
            message(Filter.Sticker) {
                bot.sendMessage(ChatId.fromId(message.chat.id), text = "You have received an awesome sticker \\o/")
            }

            message(Filter.Reply or Filter.Forward) {
                bot.sendMessage(ChatId.fromId(message.chat.id), text = "someone is replying or forwarding messages ...")
            }

            command("start") {
                val result = bot.sendMessage(chatId = ChatId.fromId(update.message!!.chat.id), text = "Bot started")

                result.fold(
                    {
                        // do something here with the response
                    },
                    {
                        // do something with the error
                    },
                )
            }

            command("hello") {
                val result = bot.sendMessage(chatId = ChatId.fromId(update.message!!.chat.id), text = "Hello, world!")

                result.fold(
                    {
                        // do something here with the response
                    },
                    {
                        // do something with the error
                    },
                )
            }

            command("commandWithArgs") {
                val joinedArgs = args.joinToString()
                val response = if (joinedArgs.isNotBlank()) joinedArgs else "There is no text apart from command!"
                bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = response)
            }

            command("markdown") {
                val markdownText = "_Cool message_: *Markdown* is `beatiful` :P"
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = markdownText,
                    parseMode = MARKDOWN,
                )
            }

            command("markdownV2") {
                val markdownV2Text = """
                    *bold \*text*
                    _italic \*text_
                    __underline__
                    ~strikethrough~
                    *bold _italic bold ~italic bold strikethrough~ __underline italic bold___ bold*
                    [inline URL](http://www.example.com/)
                    [inline mention of a user](tg://user?id=123456789)
                    `inline fixed-width code`
                    ```kotlin
                    fun main() {
                        println("Hello Kotlin!")
                    }
                    ```
                """.trimIndent()
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = markdownV2Text,
                    parseMode = MARKDOWN_V2,
                )
            }

            command("inlineButtons") {
                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(InlineKeyboardButton.CallbackData(text = "Test Inline Button", callbackData = "testButton")),
                    listOf(InlineKeyboardButton.CallbackData(text = "Show alert", callbackData = "showAlert")),
                )
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Hello, inline buttons!",
                    replyMarkup = inlineKeyboardMarkup,
                )
            }

            command("userButtons") {
                val keyboardMarkup = KeyboardReplyMarkup(keyboard = generateUsersButton(), resizeKeyboard = true)
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Hello, users buttons!",
                    replyMarkup = keyboardMarkup,
                )
            }

            command("mediaGroup") {
                bot.sendMediaGroup(
                    chatId = ChatId.fromId(message.chat.id),
                    mediaGroup = MediaGroup.from(
                        InputMediaPhoto(
                            media = ByUrl("https://avatars.mds.yandex.net/i?id=3dd03e0efc3018a6277aa9496e919da6a3bddb03-2398678-images-thumbs&n=13"),
                            caption = "I come from an url :P",
                        ),
                        InputMediaPhoto(
                            media = ByUrl("https://avatars.mds.yandex.net/i?id=3dd03e0efc3018a6277aa9496e919da6a3bddb03-2398678-images-thumbs&n=13"),
                            caption = "Me too!",
                        ),
                    ),
                    replyToMessageId = message.messageId,
                )
            }

            callbackQuery("testButton") {
                val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                bot.sendMessage(ChatId.fromId(chatId), callbackQuery.data)
            }

            callbackQuery(
                callbackData = "showAlert",
                callbackAnswerText = "HelloText",
                callbackAnswerShowAlert = true,
            ) {
                val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                bot.sendMessage(ChatId.fromId(chatId), callbackQuery.data)
            }

            text("ping") {
                bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "Pong")
            }

            location {
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Your location is (${location.latitude}, ${location.longitude})",
                    replyMarkup = ReplyKeyboardRemove(),
                )
            }

            contact {
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Hello, ${contact.firstName} ${contact.lastName}",
                    replyMarkup = ReplyKeyboardRemove(),
                )
            }

            channel {
                // Handle channel update
            }

            inlineQuery {
                val queryText = inlineQuery.query

                if (queryText.isBlank() or queryText.isEmpty()) return@inlineQuery

                val inlineResults = (0 until 5).map {
                    InlineQueryResult.Article(
                        id = it.toString(),
                        title = "$it. $queryText",
                        inputMessageContent = InputMessageContent.Text("$it. $queryText"),
                        description = "Add $it. before you word",
                    )
                }
                bot.answerInlineQuery(inlineQuery.id, inlineResults)
            }

            chosenInlineResult {
                bot.sendMessage(
                    ChatId.fromId(chosenInlineResult.from.id),
                    text = "User selected: ${chosenInlineResult.resultId}",
                )
            }

            photos {
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Wowww, awesome photos!!! :P",
                )
            }

            command("diceAsDartboard") {
                bot.sendDice(ChatId.fromId(message.chat.id), DiceEmoji.Dartboard)
            }

            dice {
                bot.sendMessage(ChatId.fromId(message.chat.id), "A dice ${dice.emoji.emojiValue} with value ${dice.value} has been received!")
            }

            telegramError {
                println(error.getErrorMessage())
            }
        }
    }

    bot.startPolling()
}

fun generateUsersButton(): List<List<KeyboardButton>> {
    return listOf(
        listOf(KeyboardButton("Request location (not supported on desktop)", requestLocation = true)),
        listOf(KeyboardButton("Request contact", requestContact = true)),
    )
}
