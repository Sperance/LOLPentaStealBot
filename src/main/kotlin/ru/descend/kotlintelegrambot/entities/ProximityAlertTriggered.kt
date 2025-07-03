package ru.descend.kotlintelegrambot.entities

import ru.descend.kotlintelegrambot.entities.User

data class ProximityAlertTriggered(
    val traveler: User,
    val watcher: User,
    val distance: Int,
)
