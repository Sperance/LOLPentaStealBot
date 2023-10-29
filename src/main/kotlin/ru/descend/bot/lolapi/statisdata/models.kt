package ru.descend.bot.lolapi.statisdata

import ru.descend.bot.lolapi.*

data class ChampionListDto(val keys: Map<String, String>,
                           val data: Map<String, ChampionDto>,
                           val version: String,
                           val type: String,
                           val format: String)

data class ChampionDto(val info: InfoDto,
                       val enemyTips: List<String>,
                       val stats: StatsDto,
                       val name: String,
                       val title: String,
                       val image: ImageDto,
                       val tags: List<String>,
                       val partype: String,
                       val skins: List<SkinDto>,
                       val passive: PassiveDto,
                       val recommended: RecommendedDto,
                       val allytips: List<String>,
                       val key: String,
                       val lore: String,
                       val id: Int,
                       val blurb: String,
                       val spells: List<ChampionSpellDto>)

data class ItemListDto(val data: Map<String, ItemDto>,
                       val version: String,
                       val tree: List<ItemTreeDto>,
                       val groups: List<GroupDto>,
                       val type: String)

data class ItemTreeDto(val header: String,
                       val tags: List<String>)

data class ItemDto(val gold: GoldDto,
                   val plaintext: String,
                   val hideFromAll: Boolean,
                   val inStore: Boolean,
                   val into: List<String>,
                   val id: Int,
                   val stats: InventoryDataStatsDto,
                   val colloq: String,
                   val map: Map<String, Boolean>,
                   val specialRecipe: Int,
                   val image: ImageDto,
                   val description: String,
                   val tags: List<String>,
                   val effect: Map<String, String>,
                   val requiredChampion: String,
                   val from: List<String>,
                   val group: String,
                   val consumeOnFull: Boolean,
                   val name: String,
                   val consumed: String,
                   val sanitizedDescription: String,
                   val depth: Int,
                   val stacks: Int)

data class GoldDto(val sell: Int,
                   val total: Int,
                   val base: Int,
                   val purchasable: Boolean)

data class InventoryDataStatsDto(val PercentCritDamageMod: Double,
                                 val PercentSpellBlockMod: Double,
                                 val PercentHPRegenMod: Double,
                                 val PercentMovementSpeedMod: Double,
                                 val FlatSpellBlockMod: Double,
                                 val FlatCritDamageMod: Double,
                                 val FlatEnergyPoolMod: Double,
                                 val PercentLifeStealMod: Double,
                                 val FlatMPPoolMod: Double,
                                 val FlatMovementSpeedMod: Double,
                                 val PercentAttackSpeedMod: Double,
                                 val FlatBlockMod: Double,
                                 val PercentBlockMod: Double,
                                 val FlatEnergyRegenMod: Double,
                                 val PercentSpellVampMod: Double,
                                 val FlatMPRegenMod: Double,
                                 val PercentDodgeMod: Double,
                                 val FlatAttackSpeedMod: Double,
                                 val FlatArmorMod: Double,
                                 val FlatHPRegenMod: Double,
                                 val PercentMagicDamageMod: Double,
                                 val PercentMPPoolMod: Double,
                                 val FlatMagicDamageMod: Double,
                                 val PercentMPRegenMod: Double,
                                 val PercentPhysicalDamageMod: Double,
                                 val FlatPhysicalDamageMod: Double,
                                 val PercentHPPoolMod: Double,
                                 val PercentArmorMod: Double,
                                 val PercentCritChanceMod: Double,
                                 val PercentEXPBonus: Double,
                                 val FlatHPPoolMod: Double,
                                 val FlatCritChanceMod: Double,
                                 val FlatEXPBonus: Double)

data class GroupDto(val MaxGroupOwnable: String,
                    val key: String)

data class LanguageStringsDto(val data: Map<String, String>,
                              val version: String,
                              val type: String)

data class MapDataDto(val data: Map<String, MapDetailsDto>,
                      val version: String,
                      val type: String)

data class MapDetailsDto(val mapName: String,
                         val image: ImageDto)

data class MasteryListDto(val data: Map<String, MasteryDto>,
                          val version: String,
                          val tree: MasteryTreeDto,
                          val type: String)

data class MasteryTreeDto(val Resolve: List<MasteryTreeListDto>,
                          val Defense: List<MasteryTreeListDto>,
                          val Utility: List<MasteryTreeListDto>,
                          val Offense: List<MasteryTreeListDto>,
                          val Ferocity: List<MasteryTreeListDto>,
                          val Cunning: List<MasteryTreeListDto>)


data class MasteryTreeListDto(val masteryTreeItems: List<MasteryTreeItemDto>)

data class MasteryTreeItemDto(val masteryId: Int,
                              val prereq: String)

data class MasteryDto(val prereq: String,
                      val masteryTree: String,
                      val name: String,
                      val ranks: String,
                      val image: ImageDto,
                      val sanitizedDescription: List<String>,
                      val id: Int,
                      val description: List<String>)

data class ProfileIconDataDto(val data: Map<String, ProfileIconDetailsDto>,
                              val version: String,
                              val type: String)

data class ProfileIconDetailsDto(val image: ImageDto,
                                 val id: Long)

data class RealmDto(val lg: String,
                    val dd: String,
                    val l: String,
                    val n: Map<String, String>,
                    val profileiconmax: Int,
                    val store: String,
                    val v: String,
                    val cdn: String,
                    val css: String)


data class ReforgedRunePathDto(val slots: List<ReforgedRuneSlotDto>,
                               val icon: String,
                               val id: Int,
                               val key: String,
                               val name: String)

data class ReforgedRuneSlotDto(val runes: List<ReforgedRuneDto>)

data class ReforgedRuneDto(val runePathName: String,
                           val runePathId: Int,
                           val name: String,
                           val id: Int,
                           val key: String,
                           val shortDesc: String,
                           val longDesc: String,
                           val icon: String)

data class RuneListDto(val data: Map<String, RuneDto>,
                       val version: String,
                       val type: String)

data class RuneDto(val stats: RuneStatsDto,
                   val name: String,
                   val tags: List<String>,
                   val image: ImageDto,
                   val sanitizedDescription: String,
                   val rune: MetaDataDto,
                   val id: Int,
                   val description: String)

data class RuneStatsDto(val PercentTimeDeadModPerLevel: Double,
                        val PercentArmorPenetrationModPerLevel: Double,
                        val PercentCritDamageMod: Double,
                        val PercentSpellBlockMod: Double,
                        val PercentHPRegenMod: Double,
                        val PercentMovementSpeedMod: Double,
                        val FlatSpellBlockMod: Double,
                        val FlatEnergyRegenModPerLevel: Double,
                        val FlatEnergyPoolMod: Double,
                        val FlatMagicPenetrationModPerLevel: Double,
                        val PercentLifeStealMod: Double,
                        val FlatMPPoolMod: Double,
                        val PercentCooldownMod: Double,
                        val PercentMagicPenetrationMod: Double,
                        val FlatArmorPenetrationModPerLevel: Double,
                        val FlatMovementSpeedMod: Double,
                        val FlatTimeDeadModPerLevel: Double,
                        val FlatArmorModPerLevel: Double,
                        val PercentAttackSpeedMod: Double,
                        val FlatDodgeModPerLevel: Double,
                        val PercentMagicDamageMod: Double,
                        val PercentBlockMod: Double,
                        val FlatDodgeMod: Double,
                        val FlatEnergyRegenMod: Double,
                        val FlatHPModPerLevel: Double,
                        val PercentAttackSpeedModPerLevel: Double,
                        val PercentSpellVampMod: Double,
                        val FlatMPRegenMod: Double,
                        val PercentHPPoolMod: Double,
                        val PercentDodgeMod: Double,
                        val FlatAttackSpeedMod: Double,
                        val FlatArmorMod: Double,
                        val FlatMagicDamageModPerLevel: Double,
                        val FlatHPRegenMod: Double,
                        val PercentPhysicalDamageMod: Double,
                        val FlatCritChanceModPerLevel: Double,
                        val FlatSpellBlockModPerLevel: Double,
                        val PercentTimeDeadMod: Double,
                        val FlatBlockMod: Double,
                        val PercentMPPoolMod: Double,
                        val FlatMagicDamageMod: Double,
                        val PercentMPRegenMod: Double,
                        val PercentMovementSpeedModPerLevel: Double,
                        val PercentCooldownModPerLevel: Double,
                        val FlatMPModPerLevel: Double,
                        val FlatEnergyModPerLevel: Double,
                        val FlatPhysicalDamageMod: Double,
                        val FlatHPRegenModPerLevel: Double,
                        val FlatCritDamageMod: Double,
                        val PercentArmorMod: Double,
                        val FlatMagicPenetrationMod: Double,
                        val PercentCritChanceMod: Double,
                        val FlatPhysicalDamageModPerLevel: Double,
                        val PercentArmorPenetrationMod: Double,
                        val PercentEXPBonus: Double,
                        val FlatMPRegenModPerLevel: Double,
                        val PercentMagicPenetrationModPerLevel: Double,
                        val FlatTimeDeadMod: Double,
                        val FlatMovementSpeedModPerLevel: Double,
                        val FlatGoldPer10Mod: Double,
                        val FlatArmorPenetrationMod: Double,
                        val FlatCritDamageModPerLevel: Double,
                        val FlatHPPoolMod: Double,
                        val FlatCritChanceMod: Double,
                        val FlatEXPBonus: Double)

data class MetaDataDto(val tier: String,
                       val type: String,
                       val isRune: Boolean)

data class SummonerSpellListDto(val data: Map<String, SummonerSpellDto>,
                                val version: String,
                                val type: String)

data class SummonerSpellDto(val vars: List<SpellVarsDto>,
                            val image: ImageDto,
                            val costBurn: String,
                            val cooldown: List<Double>,
                            val effectBurn: List<String>,
                            val id: Int,
                            val cooldownBurn: String,
                            val tooltip: String,
                            val maxrank: Int,
                            val rangeBurn: String,
                            val description: String,
                            val effect: List<List<List<Double>>>,
                            val key: String,
                            val leveltip: LevelTipDto,
                            val modes: List<String>,
                            val resource: String,
                            val name: String,
                            val costType: String,
                            val sanitizedDescription: String,
                            val sanitizedTooltip: String,
                            val range: String,
                            val cost: List<Int>,
                            val summonerLevel: Int)

data class SpellVarsDto(val ranksWith: String,
                        val dyn: String,
                        val link: String,
                        val coeff: List<Double>,
                        val key: String)