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
//        listeners.onBeforeAttack.invokeEach(this, enemy)
        val fromAttack = stats.attack.battleStat.get()
        enemy.stats.health.battleStat.rem(fromAttack)
        println("$name атаковал ${enemy.name} на $fromAttack со скоростью ${stats.attackSpeed.battleStat.get()}. Макс ХП: ${stats.health.battleStat.maximumValue}. Осталось жизней у ${enemy.name} : ${enemy.stats.health.battleStat.get()}")
//        listeners.onAttacking.invokeEach(this, enemy)
//        enemy.listeners.onAttacked.invokeEach(this, enemy)
//        listeners.onAfterAttack.invokeEach(this, enemy)
    }

    fun initForBattle() {
        stats.initForBattle()
        effects.initForBattle()
    }
}