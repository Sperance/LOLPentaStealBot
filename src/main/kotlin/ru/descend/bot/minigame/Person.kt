package ru.descend.bot.minigame

data class PersonValues(
    val lowLevelHealth: BaseProperty = BaseProperty("Нижний лимит здоровья", 0.0),
    val maxAttackSpeed: BaseProperty = BaseProperty("Максимальная скорость атаки", 500.0)
)

data class PersonBlobs(
    val isAlive: BlobProperty = BlobProperty("Живой", true)
)

data class Person(
    var name: String,
    var personValues: PersonValues = PersonValues(),
    var personBlobs: PersonBlobs = PersonBlobs()
) {
    var stats: PersonStats = PersonStats(this)

    fun initForBattle() {
        stats.initForBattle()
    }
}