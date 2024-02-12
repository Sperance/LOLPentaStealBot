package ru.descend.bot.savedObj

import ru.descend.bot.postgre.tables.TableKORD_LOL
import ru.descend.bot.postgre.tables.TableMatch
import ru.descend.bot.postgre.tables.TableMmr
import ru.descend.bot.postgre.tables.TableParticipant
import ru.descend.bot.printLog
import ru.descend.bot.to2Digits
import update
import kotlin.reflect.KMutableProperty1

class CalculateMMR(private val participant: TableParticipant, match: TableMatch, kordlol: List<TableKORD_LOL>, private val mmrTable: TableMmr?) {

    private var mmrValue = 0.0
    private var mmrText = ""
    private var mmrModificator = 1.0

    init {

        if (mmrTable != null) {
            mmrModificator = (match.matchDuration.toDouble().fromDoubleValue(mmrTable.matchDuration) / 100.0).to2Digits()
            if (mmrModificator < 0) {
                printLog("[CalculateMMR] mmrModificator < 0: $mmrModificator. Match: ${match.matchId}. Setting modificator 1.0")
                mmrModificator = 1.0
            }

            calculateField(TableParticipant::minionsKills, TableMmr::minions)
            calculateField(TableParticipant::skillsCast, TableMmr::skills)
            calculateField(TableParticipant::totalDamageShieldedOnTeammates, TableMmr::shielded)
            calculateField(TableParticipant::totalHealsOnTeammates, TableMmr::healed)
            calculateField(TableParticipant::damageDealtToBuildings, TableMmr::dmgBuilding)
            calculateField(TableParticipant::timeCCingOthers, TableMmr::controlEnemy)
            calculateField(TableParticipant::skillshotsDodged, TableMmr::skillDodge)
            calculateField(TableParticipant::enemyChampionImmobilizations, TableMmr::immobiliz)
            calculateField(TableParticipant::damageTakenOnTeamPercentage, TableMmr::dmgTakenPerc)
            calculateField(TableParticipant::teamDamagePercentage, TableMmr::dmgDealPerc)
            calculateField(TableParticipant::kda, TableMmr::kda)

            if (match.matchMode == "ARAM") {
                kordlol.find { it.LOLperson?.LOL_puuid == participant.LOLperson?.LOL_puuid }?.let {
                    it.update(TableKORD_LOL::mmrAram){
                        printLog("[CalculateMMR] Updated mmr for user KORD_LOL ${this.id} old MMR: $mmrAram adding MMR: $mmrValue")
                        mmrAram = (mmrAram + mmrValue).to2Digits()
                    }
                }
            }
        }
    }

    private fun calculateField(propertyParticipant: KMutableProperty1<TableParticipant, *>, propertyMmr: KMutableProperty1<TableMmr, *>) {
        if (mmrTable == null) return

        val valuePropertyMmr = propertyMmr.invoke(mmrTable) as Double
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
        return "CalculateMMR(Value=$mmrValue, Text='$mmrText', Modificator=$mmrModificator)"
    }
}