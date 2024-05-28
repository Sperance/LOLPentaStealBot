package ru.descend.bot.minigame

class EffectSuperSpeedDMG : BaseApplyEffect(
    "Супер пупер эффект",
    listOf(BaseEffectAdditionalDamage(5.0), BaseEffectAttackSpeedUP(50.0)),
    0,
    EnumPersonLifects.ON_DEAL_DAMAGE
)

class EffectSuperHeal : BaseApplyEffect(
    "Супер пупер эффект 2",
    listOf(BaseEffectTimeHeal(500.0)),
    1,
    EnumPersonLifects.ON_TAKE_DAMAGE
)