package ru.descend.bot.lolapi.dataclasses

data class SavedPartSteal(
    var participantId: Long,
    var puuid: String,
    var team: String,
    var timeStamp: Long
)