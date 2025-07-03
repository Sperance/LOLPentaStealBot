package ru.descend.kotlintelegrambot.entities

import ru.descend.kotlintelegrambot.entities.files.PhotoSize
import com.google.gson.annotations.SerializedName as Name

data class UserProfilePhotos(
    @Name("total_count") val totalCount: Int,
    val photos: List<List<PhotoSize>>,
)
