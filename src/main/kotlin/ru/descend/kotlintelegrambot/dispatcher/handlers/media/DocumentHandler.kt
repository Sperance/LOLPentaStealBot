package com.github.kotlintelegrambot.dispatcher.handlers.media

import com.github.kotlintelegrambot.dispatcher.handlers.HandleDocument
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.entities.files.Document

class DocumentHandler(
    handleDocument: HandleDocument,
) : MediaHandler<Document>(
    handleDocument,
    DocumentHandlerFunctions::mapMessageToDocument,
    DocumentHandlerFunctions::isUpdateDocument,
)

private object DocumentHandlerFunctions {

    fun mapMessageToDocument(message: Message): Document {
        val document = message.document
        checkNotNull(document)
        return document
    }

    fun isUpdateDocument(update: Update): Boolean = update.message?.document != null
}
