package ru.descend.bot.minigame

data class PersonValues(
    val maxAttackSpeed: BaseProperty = BaseProperty("Максимальная скорость атаки", 500.0),
    val CONSTmaxAttackSpeed: BaseProperty = BaseProperty("Лимит - Максимальная скорость атаки", 100.0)
)

data class PersonBlobs(
    val isAlive: BlobProperty = BlobProperty("Живой", true),
    val isAccessAttack: BlobProperty = BlobProperty("Может атаковать", true),

    val enableCONSTmaxAttackSpeed: BlobProperty = BlobProperty("Снят лимит на максимальную скорость атаки", false),
)

data class Person(
    val name: String,
    var personValues: PersonValues = PersonValues(),
    var personBlobs: PersonBlobs = PersonBlobs(),
) {
    var stats: PersonStats = PersonStats(this)
    var listeners: PersonListeners = PersonListeners(this)
    var effects: PersonEffects = PersonEffects(this)

    fun onAttacking(enemy: Person) {
        if (!personBlobs.isAccessAttack.get()) return
        val fromAttack = stats.attack.get()
        enemy.stats.health.remStock(fromAttack)
        println("$name атаковал ${enemy.name} на $fromAttack со скоростью ${stats.attackSpeed.get()}. Осталось жизней у ${enemy.name} : ${enemy.stats.health.get()}")
        listeners.onDealDamage.invokeEach(this, enemy)
        enemy.listeners.onTakeDamage.invokeEach(enemy, this)
    }

    fun initForBattle() {
        stats.initForBattle()
        effects.initForBattle()
    }
}