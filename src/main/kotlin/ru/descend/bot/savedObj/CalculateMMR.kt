package ru.descend.bot.savedObj

import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.tables.TableKORD_LOL
import ru.descend.bot.postgre.tables.TableMatch
import ru.descend.bot.postgre.tables.TableMmr
import ru.descend.bot.postgre.tables.TableParticipant
import ru.descend.bot.printLog
import ru.descend.bot.to2Digits
import update
import kotlin.math.abs
import kotlin.reflect.KMutableProperty1

private const val modRank = 70.0
private const val modTitle = 100.0

enum class EnumMMRRank(val nameRank: String, val minMMR: Double) {
    UNRANKED("Нет ранга", 0.0),
    PAPER_III("Бумага III", UNRANKED.minMMR + modRank),
    PAPER_II("Бумага II", PAPER_III.minMMR + modRank),
    PAPER_I("Бумага I", PAPER_II.minMMR + modRank),
    WOOD_III("Дерево III", PAPER_I.minMMR + modTitle),
    WOOD_II("Дерево II", WOOD_III.minMMR + modRank),
    WOOD_I("Дерево I", WOOD_II.minMMR + modRank),
    BRONZE_III("Бронза III", WOOD_I.minMMR + modTitle),
    BRONZE_II("Бронза II", BRONZE_III.minMMR + modRank),
    BRONZE_I("Бронза I", BRONZE_II.minMMR + modRank),
    IRON_III("Железо III", BRONZE_I.minMMR + modTitle),
    IRON_II("Железо II", IRON_III.minMMR + modRank),
    IRON_I("Железо I", IRON_II.minMMR + modRank),
    SILVER_III("Серебро III", IRON_I.minMMR + modTitle),
    SILVER_II("Серебро II", SILVER_III.minMMR + modRank),
    SILVER_I("Серебро I", SILVER_II.minMMR + modRank),
    GOLD_III("Золото III", SILVER_I.minMMR + modTitle),
    GOLD_II("Золото II", GOLD_III.minMMR + modRank),
    GOLD_I("Золото I", GOLD_II.minMMR + modRank),
    PLATINUM_III("Платина III", GOLD_I.minMMR + modTitle),
    PLATINUM_II("Платина II", PLATINUM_III.minMMR + modRank),
    PLATINUM_I("Платина I", PLATINUM_II.minMMR + modRank),
    DIAMOND_III("Алмаз III", PLATINUM_I.minMMR + modTitle),
    DIAMOND_II("Алмаз II", DIAMOND_III.minMMR + modRank),
    DIAMOND_I("Алмаз I", DIAMOND_II.minMMR + modRank),
    MASTER_III("Мастер III", DIAMOND_I.minMMR + modTitle),
    MASTER_II("Мастер II", MASTER_III.minMMR + modRank),
    MASTER_I("Мастер I", MASTER_II.minMMR + modRank),
    CHALLENGER("Челленджер", MASTER_I.minMMR + modTitle)
    ;

    companion object {
        fun getMMRRank(mmr: Double) : EnumMMRRank {
            entries.forEach {
                if (it.minMMR >= mmr) return it
            }
            return UNRANKED
        }
    }
}

class CalculateMMR(private val participant: TableParticipant, val match: TableMatch, kordlol: List<TableKORD_LOL>, private val mmrTable: TableMmr?) {

    private var mmrValue = 0.0
    private var mmrValueStock = 0.0
    private var mmrText = ""
    private var mmrModificator = 1.0
    private var countFields = 0.0

    private var baseModificator = 1.2 //30%

    init {
        if (mmrTable != null) {
            mmrModificator = (match.matchDuration.toDouble().fromDoubleValue(mmrTable.matchDuration) / 100.0).to2Digits()
            if (mmrModificator < 0) {
                printLog("[CalculateMMR] mmrModificator < 0: $mmrModificator. Match: ${match.matchId}. Setting modificator 1.0")
                mmrModificator = 1.0
            }

            var isWarriorTank = false
            var isMageSupport = false
            LeagueMainObject.catchHeroForId(participant.championId.toString())?.let {
                if (it.tags.contains("Fighter") || it.tags.contains("Tank")) isWarriorTank = true
                if (it.tags.contains("Mage") || it.tags.contains("Support")) isMageSupport = true
            }

            mmrText += ";isWarriorTank=$isWarriorTank"
            mmrText += ";isMageSupport=$isMageSupport"

            countFields = 0.0
            calculateField(TableParticipant::minionsKills, TableMmr::minions)
            calculateField(TableParticipant::skillsCast, TableMmr::skills)

            if (isMageSupport) {
                calculateField(TableParticipant::totalDamageShieldedOnTeammates, TableMmr::shielded)
                calculateField(TableParticipant::totalHealsOnTeammates, TableMmr::healed)
            }

//          calculateField(TableParticipant::damageDealtToBuildings, TableMmr::dmgBuilding)
            calculateField(TableParticipant::timeCCingOthers, TableMmr::controlEnemy)

            if (isWarriorTank) {
                calculateField(TableParticipant::enemyChampionImmobilizations, TableMmr::immobiliz)
                calculateField(TableParticipant::damageTakenOnTeamPercentage, TableMmr::dmgTakenPerc)
            } else {
                calculateField(TableParticipant::skillshotsDodged, TableMmr::skillDodge)
            }

            calculateField(TableParticipant::teamDamagePercentage, TableMmr::dmgDealPerc)
            calculateField(TableParticipant::kda, TableMmr::kda)

            kordlol.find { it.LOLperson?.LOL_puuid == participant.LOLperson?.LOL_puuid }?.let {
                calculateMMRaram(it)
            }
        }
    }

    private fun calculateMMRaram(kordlol: TableKORD_LOL?) {
        if (kordlol == null) return
        if (match.matchMode != "ARAM") return
        if (match.surrender) return
        if (match.bots) return

        val currentOldRank = EnumMMRRank.getMMRRank(kordlol.mmrAram)

        val partMMR: Double
        var newSavedMMR = kordlol.mmrAramSaved.to2Digits()
        val newAramValue: Double
        if (participant.win) {
            newAramValue = (kordlol.mmrAram + mmrValue).to2Digits()
            partMMR = mmrValue.to2Digits()
        } else {
            var minusMMR = (if (mmrValue < countFields) (countFields - mmrValue) * 2.0
            else 1.0).to2Digits()
            partMMR = -minusMMR.to2Digits()

            if (newSavedMMR > 0.0) {
                newSavedMMR -= minusMMR
            }
            if (newSavedMMR < 0.0) {
                minusMMR -= abs(newSavedMMR)
                if (minusMMR < 0.0) minusMMR = 0.0
                newSavedMMR = 0.0
            }

            newAramValue = (if (kordlol.mmrAram - minusMMR < 0.0) 0.0
            else kordlol.mmrAram - minusMMR).to2Digits()
        }

        kordlol.update(TableKORD_LOL::mmrAram, TableKORD_LOL::mmrAramMaxRank, TableKORD_LOL::mmrAramSaved){
            mmrAram = newAramValue
            mmrAramSaved = newSavedMMR.to2Digits()

            val currentNewRank = EnumMMRRank.getMMRRank(newAramValue)
            mmrAramMaxRank = if (currentOldRank.minMMR < currentNewRank.minMMR){
                val maxRank = EnumMMRRank.entries.find { it.nameRank == mmrAramMaxRank }
                if (maxRank == null) currentNewRank.nameRank
                else currentNewRank.nameRank
            } else if (mmrAramMaxRank.isBlank()) {
                currentNewRank.nameRank
            } else {
                mmrAramMaxRank
            }
        }
        participant.update(TableParticipant::mmr){
            mmr = partMMR
        }
    }

    private fun calculateField(propertyParticipant: KMutableProperty1<TableParticipant, *>, propertyMmr: KMutableProperty1<TableMmr, *>) {
        if (mmrTable == null) return

        countFields++
        val valuePropertyMmr = ((propertyMmr.invoke(mmrTable) as Double) * baseModificator).to2Digits()
        val valuePropertyMmrStock = (propertyMmr.invoke(mmrTable) as Double).to2Digits()
        val valuePropertyParticipant = when (val valuePart = propertyParticipant.invoke(participant)){
            is Int -> valuePart.toDouble()
            is Double -> valuePart
            is Long -> valuePart.toDouble()
            else -> {
                printLog("[calculateField] Fail convert field Participant ${propertyParticipant.name} to Double: $valuePart")
                0.0
            }
        }.to2Digits()

        val localMMR = valuePropertyParticipant.fromDoublePerc(valuePropertyMmr * mmrModificator).to2Digits()
        mmrValue += localMMR
        mmrValue = mmrValue.to2Digits()

        val localMMRStock = valuePropertyParticipant.fromDoublePerc(valuePropertyMmrStock * mmrModificator).to2Digits()
        mmrValueStock += localMMRStock
        mmrValueStock = mmrValueStock.to2Digits()

        if (localMMR > 0.0)
            mmrText += ";${propertyMmr.name}:$valuePropertyParticipant(${(valuePropertyMmr * mmrModificator).to2Digits()})=$localMMR"
    }

    private fun Double.fromDoubleValue(stock: Double): Double {
        return ((this / stock) * 100.0)
    }

    private fun Double.fromDoublePerc(stock: Double): Double {
        // 40 крипов - по стате 200 норма - это 20% от нормы
        return when ((this / stock) * 100.0) {
            in Double.MIN_VALUE..10.0 -> 0.0
            in 10.0..20.0 -> 0.2
            in 20.0..40.0 -> 0.4
            in 40.0..60.0 -> 0.6
            in 60.0..80.0 -> 0.8
            in 80.0..100.0 -> 1.0
            in 100.0..140.0 -> 1.2
            in 140.0..180.0 -> 1.4
            in 180.0..220.0 -> 1.6
            in 220.0..260.0 -> 1.8
            in 260.0..Double.MAX_VALUE -> 2.0
            else -> 0.0
        }
    }

    override fun toString(): String {
        return "CalculateMMR(mmrValue=$mmrValue, mmrValueStock=$mmrValueStock, mmrText='$mmrText', mmrModificator=$mmrModificator, baseModificator=$baseModificator, countFields=$countFields)"
    }
}