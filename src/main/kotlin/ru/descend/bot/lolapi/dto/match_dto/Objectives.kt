package ru.descend.bot.lolapi.dto.match_dto

data class Objectives(
    val baron: Baron,
    val champion: Champion,
    val dragon: Dragon,
    val horde: Horde,
    val inhibitor: Inhibitor,
    val riftHerald: RiftHerald,
    val tower: Tower
)