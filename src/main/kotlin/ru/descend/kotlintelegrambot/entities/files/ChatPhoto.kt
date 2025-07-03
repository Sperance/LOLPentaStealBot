package ru.descend.kotlintelegrambot.entities.files

import com.google.gson.annotations.SerializedName
import ru.descend.kotlintelegrambot.entities.files.FilesFields

/**
 * Represents a chat photo.
 * https://core.telegram.org/bots/api#chatphoto
 */
data class ChatPhoto(
    @SerializedName(FilesFields.smallFileId) val smallFileId: String,
    @SerializedName(FilesFields.smallFileUniqueId) val smallFileUniqueId: String,
    @SerializedName(FilesFields.bigFileId) val bigFileId: String,
    @SerializedName(FilesFields.bigFileUniqueId) val bigFileUniqueId: String,
)
