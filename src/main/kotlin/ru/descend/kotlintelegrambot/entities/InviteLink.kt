package ru.descend.kotlintelegrambot.entities

import com.google.gson.annotations.SerializedName
import ru.descend.kotlintelegrambot.entities.User

data class InviteLink(
    @SerializedName("invite_link")
    val inviteLink: String,
    @SerializedName("creator")
    val creator: User,
)
