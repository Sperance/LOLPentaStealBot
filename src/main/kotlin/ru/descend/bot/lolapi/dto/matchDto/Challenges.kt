package ru.descend.bot.lolapi.dto.matchDto

data class Challenges(
    val `12AssistStreakCount`: Int,
    val InfernalScalePickup: Int,
    val abilityUses: Int,
    val acesBefore15Minutes: Int,
    val alliedJungleMonsterKills: Double,
    val baronTakedowns: Int,
    val blastConeOppositeOpponentCount: Int,
    val bountyGold: Double,
    val buffsStolen: Int,
    val earliestBaron: Double,
    val baronBuffGoldAdvantageOverThreshold: Int,
    val completeSupportQuestInTime: Int,
    val controlWardTimeCoverageInRiverOrEnemyHalf: Double,
    val controlWardsPlaced: Int,
    val damagePerMinute: Double,
    val damageTakenOnTeamPercentage: Double,
    val dancedWithRiftHerald: Int,
    val deathsByEnemyChamps: Int,
    val dodgeSkillShotsSmallWindow: Int,
    val doubleAces: Int,
    val dragonTakedowns: Int,
    val earliestDragonTakedown: Double,
    val earliestElderDragon: Double,
    val earlyLaningPhaseGoldExpAdvantage: Int,
    val effectiveHealAndShielding: Double,
    val elderDragonKillsWithOpposingSoul: Int,
    val elderDragonMultikills: Int,
    val enemyChampionImmobilizations: Int,
    val enemyJungleMonsterKills: Double,
    val epicMonsterKillsNearEnemyJungler: Int,
    val epicMonsterKillsWithin30SecondsOfSpawn: Int,
    val epicMonsterSteals: Int,
    val epicMonsterStolenWithoutSmite: Int,
    val fasterSupportQuestCompletion: Int,
    val fastestLegendary: Double,
    val hadAfkTeammate: Int,
    val firstTurretKilled: Double,
    val firstTurretKilledTime: Double,
    val fistBumpParticipation: Int,
    val flawlessAces: Int,
    val fullTeamTakedown: Int,
    val gameLength: Double,
    val getTakedownsInAllLanesEarlyJungleAsLaner: Int,
    val goldPerMinute: Double,
    val hadOpenNexus: Int,
    val highestChampionDamage: Int,
    val highestCrowdControlScore: Int,
    val highestWardKills: Int,
    val immobilizeAndKillWithAlly: Int,
    val initialBuffCount: Int,
    val initialCrabCount: Int,
    val jungleCsBefore10Minutes: Double,
    val junglerKillsEarlyJungle: Int,
    val junglerTakedownsNearDamagedEpicMonster: Int,
    val kTurretsDestroyedBeforePlatesFall: Int,
    val kda: Double,
    val killAfterHiddenWithAlly: Int,
    val killParticipation: Double,
    val killedChampTookFullTeamDamageSurvived: Int,
    val killingSprees: Int,
    val killsNearEnemyTurret: Int,
    val killsOnLanersEarlyJungleAsJungler: Int,
    val killsOnOtherLanesEarlyJungleAsLaner: Int,
    val killsOnRecentlyHealedByAramPack: Int,
    val killsUnderOwnTurret: Int,
    val killsWithHelpFromEpicMonster: Int,
    val knockEnemyIntoTeamAndKill: Int,
    val landSkillShotsEarlyGame: Int,
    val laneMinionsFirst10Minutes: Int,
    val laningPhaseGoldExpAdvantage: Int,
    val legendaryCount: Int,
    val legendaryItemUsed: List<Int>,
    val lostAnInhibitor: Int,
    val maxCsAdvantageOnLaneOpponent: Double,
    val maxKillDeficit: Int,
    val maxLevelLeadLaneOpponent: Int,
    val mostWardsDestroyedOneSweeper: Int,
    val mythicItemUsed: Int,
    val mejaisFullStackInTime: Double,
    val moreEnemyJungleThanOpponent: Double,
    val multiKillOneSpell: Int,
    val multiTurretRiftHeraldCount: Int,
    val multikills: Int,
    val multikillsAfterAggressiveFlash: Int,
    val outerTurretExecutesBefore10Minutes: Int,
    val outnumberedKills: Int,
    val outnumberedNexusKill: Int,
    val perfectDragonSoulsTaken: Int,
    val perfectGame: Int,
    val pickKillWithAlly: Int,
    val playedChampSelectPosition: Int,
    val poroExplosions: Int,
    val quickCleanse: Int,
    val quickFirstTurret: Int,
    val quickSoloKills: Int,
    val riftHeraldTakedowns: Int,
    val saveAllyFromDeath: Int,
    val scuttleCrabKills: Int,
    val shortestTimeToAceFromFirstTakedown: Double,
    val skillshotsDodged: Int,
    val skillshotsHit: Int,
    val snowballsHit: Int,
    val soloBaronKills: Int,
    val soloKills: Int,
    val soloTurretsLategame: Int,
    val takedownsFirst25Minutes: Int,
    val teleportTakedowns: Int,
    val thirdInhibitorDestroyedTime: Double,
    val stealthWardsPlaced: Int,
    val threeWardsOneSweeperCount: Int,
    val survivedSingleDigitHpCount: Int,
    val survivedThreeImmobilizesInFight: Int,
    val takedownOnFirstTurret: Int,
    val takedowns: Int,
    val takedownsAfterGainingLevelAdvantage: Int,
    val takedownsBeforeJungleMinionSpawn: Int,
    val takedownsFirstXMinutes: Int,
    val takedownsInAlcove: Int,
    val takedownsInEnemyFountain: Int,
    val teamBaronKills: Int,
    val teamDamagePercentage: Double,
    val teamElderDragonKills: Int,
    val teamRiftHeraldKills: Int,
    val tookLargeDamageSurvived: Int,
    val turretPlatesTaken: Int,
    val turretTakedowns: Int,
    val turretsTakenWithRiftHerald: Int,
    val twentyMinionsIn3SecondsCount: Int,
    val twoWardsOneSweeperCount: Int,
    val unseenRecalls: Int,
    val visionScoreAdvantageLaneOpponent: Double,
    val voidMonsterKill: Int,
    val wardTakedowns: Int,
    val wardTakedownsBefore20M: Int,
    val wardsGuarded: Int
)