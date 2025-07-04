package ru.descend.kotlintelegrambot.network.multipart

import ru.descend.kotlintelegrambot.entities.ChatId
import ru.descend.kotlintelegrambot.entities.TelegramFile
import ru.descend.kotlintelegrambot.entities.inputmedia.InputMediaDocument
import ru.descend.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import ru.descend.kotlintelegrambot.entities.inputmedia.InputMediaVideo
import ru.descend.kotlintelegrambot.entities.inputmedia.MediaGroup
import ru.descend.kotlintelegrambot.network.ApiConstants
import ru.descend.kotlintelegrambot.network.MediaTypeConstants
import ru.descend.kotlintelegrambot.network.retrofit.converters.ChatIdConverterFactory
import com.google.gson.Gson
import okhttp3.MultipartBody
import java.io.File

internal class MultipartBodyFactory(private val gson: Gson) {

    fun createForSendMediaGroup(
        chatId: ChatId,
        mediaGroup: MediaGroup,
        disableNotification: Boolean? = null,
        protectContent: Boolean? = null,
        replyToMessageId: Long? = null,
        allowSendingWithoutReply: Boolean?,
    ): List<MultipartBody.Part> {
        val chatIdString = ChatIdConverterFactory.chatIdToString(chatId)
        val chatIdPart = chatIdString.toMultipartBodyPart(ApiConstants.CHAT_ID)
        return createSendMediaGroupMultipartBody(chatIdPart, mediaGroup, disableNotification, protectContent, replyToMessageId, allowSendingWithoutReply)
    }

    private fun createSendMediaGroupMultipartBody(
        chatIdPart: MultipartBody.Part,
        mediaGroup: MediaGroup,
        disableNotification: Boolean? = null,
        protectContent: Boolean? = null,
        replyToMessageId: Long? = null,
        allowSendingWithoutReply: Boolean?,
    ): List<MultipartBody.Part> {
        val filesParts = mediaGroup.takeFiles().map { (file, mediaType) ->
            file.toMultipartBodyPart(mediaType = mediaType)
        }
        val mediaGroupPart = gson.toJson(mediaGroup.medias).toMultipartBodyPart(ApiConstants.SendMediaGroup.MEDIA)
        val disableNotificationPart = disableNotification?.toMultipartBodyPart(ApiConstants.DISABLE_NOTIFICATION)
        val protectContentPart = protectContent?.toMultipartBodyPart(ApiConstants.PROTECT_CONTENT)
        val replyToMessageIdPart = replyToMessageId?.toMultipartBodyPart(ApiConstants.REPLY_TO_MESSAGE_ID)
        val allowSendingWithoutReplyPart = allowSendingWithoutReply?.toMultipartBodyPart(
            ApiConstants.ALLOW_SENDING_WITHOUT_REPLY)

        return listOfNotNull(chatIdPart, mediaGroupPart, disableNotificationPart, protectContentPart, replyToMessageIdPart, allowSendingWithoutReplyPart) + filesParts
    }

    private fun MediaGroup.takeFiles(): List<Pair<File, String>> = medias.flatMap { groupableMedia ->
        when {
            groupableMedia is InputMediaDocument && groupableMedia.media is TelegramFile.ByFile -> listOf(
                groupableMedia.media.file to MediaTypeConstants.DOCUMENT,
            )
            groupableMedia is InputMediaPhoto && groupableMedia.media is TelegramFile.ByFile -> listOf(
                groupableMedia.media.file to MediaTypeConstants.IMAGE,
            )
            groupableMedia is InputMediaVideo && groupableMedia.media is TelegramFile.ByFile && groupableMedia.thumb != null -> listOf(
                groupableMedia.media.file to MediaTypeConstants.VIDEO,
                groupableMedia.thumb.file to MediaTypeConstants.IMAGE,
            )
            groupableMedia is InputMediaVideo && groupableMedia.media is TelegramFile.ByFile -> listOf(
                groupableMedia.media.file to MediaTypeConstants.VIDEO,
            )
            groupableMedia is InputMediaVideo && groupableMedia.thumb != null -> listOf(
                groupableMedia.thumb.file to MediaTypeConstants.IMAGE,
            )
            else -> emptyList()
        }
    }
}
