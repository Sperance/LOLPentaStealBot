package ru.descend.kotlintelegrambot.entities.payments

data class ShippingOption(
    val id: String,
    val title: String,
    val prices: List<LabeledPrice>,
)
