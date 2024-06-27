package ru.descend.bot.lolapi.dto.matchDto

data class Style(
    val description: String,
    val selections: List<Selection>,
    val style: Int
)