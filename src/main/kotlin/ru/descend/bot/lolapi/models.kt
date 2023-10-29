package ru.descend.bot.lolapi

data class ChampionMasteryDTO(val chestGranted: Boolean,
                              val championLevel: Int,
                              val championPoints: Int,
                              val championId: Long,
                              val playerId: Long,
                              val championPointsUntilNextLevel: Long,
                              val tokensEarned: Int,
                              val championPointsSinceLastLevel: Int,
                              val lastPlayTime: Int)

data class LeagueListDTO(val leagueId: String,
                         val tier: String,
                         val entries: List<LeagueItemDTO>,
                         val queue: String,
                         val name: String)

data class ChampionRotationsDTO(
    val maxNewPlayerLevel: Int,
    val freeChampionIdsForNewPlayers: List<Int>,
    val freeChampionIds: List<Int>
)

data class LeagueItemDTO(val rank: String,
                         val hotStreak: Boolean,
                         val miniSeries: MiniSeriesDTO,
                         val wins: Int,
                         val veteran: Boolean,
                         val losses: Int,
                         val freshBlood: Boolean,
                         val playerOrTeamName: String,
                         val inactive: Boolean,
                         val playerOrTeamId: String,
                         val leaguePoints: Int)

data class LeaguePositionDTO(val rank: String,
                             val queueType: String,
                             val hotStreak: Boolean,
                             val miniSeries: MiniSeriesDTO,
                             val wins: Int,
                             val veteran: Boolean,
                             val losses: Int,
                             val freshBlood: Boolean,
                             val leagueId: String,
                             val playerOrTeamName: String,
                             val leagueName: String,
                             val tier: String,
                             val leaguePoints: Int)

data class MiniSeriesDTO(val wins: Int,
                         val losses: Int,
                         val target: Int,
                         val progress: String)

data class InfoDto(val difficulty: Int,
                   val attack: Int,
                   val deffense: Int,
                   val magic: Int)

data class StatsDto(val armorperlevel: Double,
                    val hpperlevel: Double,
                    val attackdamage: Double,
                    val mpperlevel: Double,
                    val attackspeedoffset: Double,
                    val armor: Double,
                    val hp: Double,
                    val hpregenperlevel: Double,
                    val spellblock: Double,
                    val attackrange: Double,
                    val movespeed: Double,
                    val attackdamageperlevel: Double,
                    val mpregenperlevel: Double,
                    val mp: Double,
                    val spellblockperlevel: Double,
                    val crit: Double,
                    val mpregen: Double,
                    val attackspeedperlevel: Double,
                    val hpregen: Double,
                    val critperlevel: Double)

data class ImageDto(val full: String,
                    val group: String,
                    val sprite: String,
                    val h: Int,
                    val w: Int,
                    val y: Int,
                    val x: Int)

data class SkinDto(val num: Int,
                   val name: String,
                   val id: Int)

data class PassiveDto(val image: ImageDto,
                      val sanitizedDescription: String,
                      val name: String,
                      val description: String)

data class RecommendedDto(val map: String,
                          val blocks: List<BlockDto>,
                          val champion: String,
                          val title: String,
                          val priority: Boolean,
                          val mode: String,
                          val type: String)

data class BlockDto(val items: List<BlockItemDto>,
                    val recMath: Boolean,
                    val type: String)

data class BlockItemDto(val count: Int,
                        val id: Int)

data class ChampionSpellDto(val cooldownBurn: String,
                            val resource: String,
                            val leveltip: LevelTipDto,
                            val vars: List<SpellVarsDto>,
                            val costType: String,
                            val image: ImageDto,
                            val sanitizedDescription: String,
                            val sanitizedTooltip: String,
                            val effect: List<List<Double>>,
                            val tooltip: String,
                            val maxrank: Int,
                            val costBurn: String,
                            val rangeBurn: String,
                            val range: List<Any>,
                            val cooldown: List<Double>,
                            val cost: List<Int>,
                            val key: String,
                            val description: String,
                            val effectBurn: List<String>,
                            val altimages: List<ImageDto>,
                            val name: String)

data class LevelTipDto(val effects: List<String>,
                       val label: List<String>)

data class SpellVarsDto(val ranksWith: String,
                        val dyn: String,
                        val link: String,
                        val coeff: List<Double>,
                        val key: String)

data class ShardStatus(val name: String,
                       val region_tag: String,
                       val hostname: String,
                       val services: List<Service>,
                       val slug: String,
                       val locales: List<String>)

data class Service(val status: String,
                   val incident: List<Incident>,
                   val name: String,
                   val slug: String)

data class Incident(val active: Boolean,
                    val created_at: String,
                    val id: Long,
                    val updates: List<Message>)

data class Message(val severity: String,
                   val author: String,
                   val created_at: String,
                   val translation: List<Translation>,
                   val updated_at: String,
                   val content: String,
                   val id: String)

data class Translation(val locale: String,
                       val content: String,
                       val created_at: String)

data class MatchDTO(val seasonId: Int,
                    val queue: Int,
                    val gameId: Long,
                    val participantIdentities: List<ParticipantIdentityDTO>,
                    val gameVersion: String,
                    val platformId: String,
                    val gameMode: String,
                    val mapId: Int,
                    val gameType: String,
                    val teams: List<TeamStatsDto>,
                    val participants: List<ParticipantDto>,
                    val gameDuration: Long,
                    val gameCreation: Long)

data class ParticipantIdentityDTO(val player: PlayerDto,
                                  val participantId: Int)

data class PlayerDto(val currentPlatformId: String,
                     val summonerName: String,
                     val matchHistoryUri: String,
                     val platformId: String,
                     val currentAccountId: Long,
                     val profileIcon: Int,
                     val summonerId: Long,
                     val accountId: Long)

data class TeamStatsDto(val firstDragon: Boolean,
                        val firstInhibitor: Boolean,
                        val bans: List<TeamBansDto>,
                        val baronKills: Int,
                        val firstRifthHerald: Boolean,
                        val firstBaron: Boolean,
                        val firstHeraldKills: Int,
                        val firstBlood: Boolean,
                        val teamId: Int,
                        val firstTower: Boolean,
                        val vilemawKills: Int,
                        val inhibitorKills: Int,
                        val towerKills: Int,
                        val dominionVictoryScore: Int,
                        val win: String,
                        val dragonKills: Int)

data class TeamBansDto(val pickTurn: Int,
                       val championId: Int)

data class ParticipantDto(val stats: ParticipantStatsDto,
                          val participantId: Int,
                          val runes: List<RuneDto>,
                          val timeline: ParticipantTimelineDto,
                          val teamId: Int,
                          val spell2Id: Int,
                          val masteries: List<MasteryDto>,
                          val highestAchievedSeasonTier: String,
                          val spell1Id: Int,
                          val championId: Int)

data class ParticipantStatsDto(val physicalDamageDealt: Long,
                               val neutralMinionsKilledTeamJungle: Int,
                               val magicDamageDealt: Long,
                               val totalPlayerScore: Int,
                               val deaths: Int,
                               val win: Boolean,
                               val neutralMinionsKilledEnemyJungle: Int,
                               val altarsCaptured: Int,
                               val largestCriticalStrike: Int,
                               val totalDamageDelt: Int,
                               val magicDamageDealtToChampions: Long,
                               val visionWardsBoughtInGame: Int,
                               val damageDealtToObjectives: Long,
                               val largestKillingSpree: Int,
                               val item1: Int,
                               val quadraKills: Int,
                               val teamObjective: Int,
                               val totalTimeCrowdControlDealt: Int,
                               val longestTimeSpentLiving: Int,
                               val wardsKilled: Int,
                               val firstTowerAssist: Boolean,
                               val firstTowerKill: Boolean,
                               val item2: Int,
                               val item3: Int,
                               val item0: Int,
                               val firstBloodAssist: Boolean,
                               val visionScore: Long,
                               val wardsPlaced: Int,
                               val item4: Int,
                               val item5: Int,
                               val item6: Int,
                               val turretKills: Int,
                               val tripleKills: Int,
                               val damageSelfMitigated: Long,
                               val champLevel: Int,
                               val nodeNeutralizeAssist: Int,
                               val firstInhibitorKill: Boolean,
                               val goldEarned: Int,
                               val magicalDamageTaken: Long,
                               val kills: Int,
                               val doubleKills: Int,
                               val nodeCaptureAssist: Int,
                               val trueDamageTaken: Long,
                               val nodeNeutralize: Int,
                               val firstInhibitorAssist: Boolean,
                               val assist: Int,
                               val unrealKills: Int,
                               val neutralMinionsKilled: Int,
                               val objectivePlayerScore: Int,
                               val combatPlayerScore: Int,
                               val damageDealtToTurrets: Long,
                               val altarsNeutralized: Int,
                               val physicalDamageDealtToChampions: Long,
                               val goldSpent: Int,
                               val trueDamageDealt: Long,
                               val trueDamageDealtToChampions: Long,
                               val participantId: Int,
                               val pentaKills: Int,
                               val totalHeal: Long,
                               val totalMinionsKilled: Int,
                               val firstBloodKill: Boolean,
                               val nodeCapture: Int,
                               val largestMultiKill: Int,
                               val sightWardsBoughtInGame: Int,
                               val totalDamageDealtToChampions: Long,
                               val totalUnitsHealed: Int,
                               val inhibitorKills: Int,
                               val totalScoreRank: Int,
                               val totalDamageTaken: Long,
                               val killingSprees: Int,
                               val timeCCingOthers: Long,
                               val physicalDamageTaken: Long)

data class RuneDto(val runeId: Int,
                   val rank: Int)

data class ParticipantTimelineDto(val lane: String,
                                  val participantId: Int,
                                  val csDiffPerMinDeltas: Map<String, Double>,
                                  val goldPerMinDeltas: Map<String, Double>,
                                  val xpDiffPerMinDeltas: Map<String, Double>,
                                  val creepsPerMinDeltas: Map<String, Double>,
                                  val xpPerMinDeltas: Map<String, Double>,
                                  val role: String,
                                  val damageTakenDiffPerMinDeltas: Map<String, Double>,
                                  val damageTakenPerMinDeltas: Map<String, Double>)

data class MasteryDto(val masteryId: Int,
                      val rank: Int)

data class MatchlistDto(val matches: List<MatchReferenceDto>,
                        val totalGames : Int,
                        val startIndex : Int,
                        val endIndex : Int)


data class MatchReferenceDto(val lane: String,
                             val gameId: Long,
                             val champion: Int,
                             val platformId: String,
                             val season: Int,
                             val queue: Int,
                             val role: String,
                             val timestamp: Long)

data class MatchTimelineDto(val frames: List<MatchFrameDto>,
                            val frameInterval: Long)

data class MatchFrameDto(val timestamp: Long,
                         val participantFrames: Map<Int, MatchParticipantFrameDto>,
                         val events: List<MatchEventDto>)

data class MatchParticipantFrameDto(val totalGold: Int,
                                    val teamScore: Int,
                                    val participantId: Int,
                                    val level: Int,
                                    val currentGold: Int,
                                    val minionsKilled: Int,
                                    val dominionScore: Int,
                                    val position: List<MatchPositionDto>,
                                    val exp: Int,
                                    val jungleMinionsKilled: Int)

data class MatchPositionDto(val y: Int,
                            val x: Int)


data class MatchEventDto(val eventType: String,
                         val towerType: String,
                         val teamId: Int,
                         val ascendedType: String,
                         val killerId: Int,
                         val levelUpType: String,
                         val pointCaptured: String,
                         val assistingParticipantIds: List<Int>,
                         val wardType: String,
                         val monsterType: String,
                         val type: String,
                         val killSlot: Int,
                         val victimId: Int,
                         val timestamp: Long,
                         val afterId: Int,
                         val monsterSubType: Int,
                         val laneType: String,
                         val itemId: Int,
                         val participantId: Int,
                         val buildingType: String,
                         val creatorId: Int,
                         val position: MatchPositionDto,
                         val beforeId: Int)

data class CurrentGameInfo(val gameId: Long,
                           val gameStartTime: Long,
                           val platformId: String,
                           val gameMode: String,
                           val mapId: Long,
                           val gameType: String,
                           val bannedChampions: List<BannedChampion>,
                           val observers: Observer,
                           val participants: List<CurrentGameParticipant>,
                           val gameLength: Long,
                           val gameQueueConfigId: Long)

data class BannedChampion(val pickTurn: Int,
                          val championId: Long,
                          val teamId: Long)

data class Observer(val encryptionKey: String)

data class CurrentGameParticipant(val profileIconId: Long,
                                  val championId: Long,
                                  val summonerName: String,
                                  val gameCustomizationObjects: List<GameCustomizationObject>,
                                  val bot: Boolean,
                                  val perks: Perks,
                                  val spell2Id: Long,
                                  val teamId: Long,
                                  val spell1Id: Long,
                                  val summonerId: Long)

data class GameCustomizationObject(val category: String,
                                   val content: String)

data class Perks(val perkStyle: Long,
                 val perkIds: List<Long>,
                 val perkSubStyle: Long)

data class FeaturedGames(val clientRefreshalInterval: Long,
                         val gameList: List<FeaturedGameInfo>)

data class FeaturedGameInfo(val gameId: Long,
                            val gameStartTime: Long,
                            val platformId: String,
                            val gameMode: String,
                            val mapId: Long,
                            val gameType: String,
                            val bannedChampions: List<BannedChampion>,
                            val observers: Observer,
                            val participants: List<Participant>,
                            val gameLength: Long,
                            val gameQueueConfigId: Long)

data class Participant(val profileIconId: Long,
                       val championId: Long,
                       val summonerName: String,
                       val bot: Boolean,
                       val spell2Id: Long,
                       val teamId: Long,
                       val spell1Id: Long)

data class SummonerDto(
    val profileIconId: Int,
    val name: String,
    val summonerLevel: Long,
    val revisionDate: Long,
    val id: String,
    val accountId: String,
    val puuid: String
)
