package ru.descend.bot.postgre.calculating

import ru.descend.bot.enums.EnumMMRRank
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.printLog
import ru.descend.bot.to1Digits
import kotlin.math.max
import kotlin.reflect.KMutableProperty1

class Calc_MMR(private var participant: Participants, var match: Matches, var lols: LOLs, private var mmrTable: MMRs?) {

    var mmrValue = 0.0
    private var mmrEmailText = ""
    private var mmrModificator = 1.0
    private var stockGainMMR = 0.0
    private var maxMMR = 0.0

    private var baseModificator = 1.2

    fun getSavedParticipant() = participant
    fun getSavedLOL() = lols

    fun init() {
        if (mmrTable != null) {
            mmrModificator = (match.matchDuration.toDouble().fromDoubleValue(mmrTable!!.matchDuration) / 100.0).to1Digits()
            if (mmrModificator < 0) {
                mmrModificator = 1.0
            }

            calculateField(Participants::minionsKills, MMRs::minions, minValue = 10.0)
            calculateField(Participants::skillsCast, MMRs::skills, maxMMR = 1.5)

            calculateField(Participants::totalDamageShieldedOnTeammates, MMRs::shielded, minValue = 500.0, maxMMR = 1.0)
            calculateField(Participants::totalHealsOnTeammates, MMRs::healed, minValue = 500.0, maxMMR = 1.0)

            calculateField(Participants::damageDealtToBuildings, MMRs::dmgBuilding, minValue = 500.0, maxMMR = 1.0)
            calculateField(Participants::timeCCingOthers, MMRs::controlEnemy, minValue = 5.0)

            calculateField(Participants::enemyChampionImmobilizations, MMRs::immobiliz, minValue = 5.0, maxMMR = 1.0)
            calculateField(Participants::damageTakenOnTeamPercentage, MMRs::dmgTakenPerc, maxMMR = 1.0)
            calculateField(Participants::skillshotsDodged, MMRs::skillDodge, minValue = 30.0, maxMMR = 1.0)

            calculateField(Participants::teamDamagePercentage, MMRs::dmgDealPerc)
            calculateField(Participants::kda, MMRs::kda)

            calculateMMRaram(lols)
        }
    }

    private fun calculateMMRaram(lols: LOLs) {
        if (match.surrender) return
        if (match.bots) return
        if (match.aborted) return

        if (participant.win) {
            stockGainMMR = mmrValue.to1Digits()
        } else {
            calcRemoveMMR(lols)
        }

        participant.gameMatchMmr = stockGainMMR.to1Digits()
        participant.gameMatchKey = ""
    }

    /**
     * Подсчет ММР которое вычитается из игрока (при поражении)
     */
    private fun calcRemoveMMR(lols: LOLs) {
        var value: Double
        val rank = EnumMMRRank.getMMRRank(lols.mmrAram)
        val tempMaxMMR = maxMMR - rank.rankValue

        value = if (tempMaxMMR > mmrValue) {
            (tempMaxMMR - mmrValue) * (0.3 + rank.rankValue / 10.0)
        } else {
            1.0
        }.to1Digits()

        if (value > tempMaxMMR) value = tempMaxMMR
        if (value < 1) value = 1.0

        stockGainMMR = -value.to1Digits()
    }

    private fun calculateField(propertyParticipant: KMutableProperty1<Participants, *>, propertyMmr: KMutableProperty1<MMRs, *>, minValue: Double? = null, maxMMR: Double? = null) {
        if (mmrTable == null) return

        val valuePropertyMmr = ((propertyMmr.invoke(mmrTable!!) as Double) * baseModificator).to1Digits()
        val valuePropertyParticipant = when (val valuePart = propertyParticipant.invoke(participant)){
            is Int -> valuePart.toDouble()
            is Double -> valuePart
            is Long -> valuePart.toDouble()
            else -> {
                printLog("[calculateField] Fail convert field Participant ${propertyParticipant.name} to Double: $valuePart")
                0.0
            }
        }.to1Digits()

        if (minValue != null && valuePropertyMmr < (minValue * mmrModificator).to1Digits()) {
            mmrEmailText += "[FIELDS] ${propertyParticipant.name} текущее значение $valuePropertyMmr меньше требуемого: ${(minValue * mmrModificator).to1Digits()}. пропускаем поле\n"
            return
        }

        var localMMR = valuePropertyParticipant.fromDoublePerc(valuePropertyMmr * mmrModificator).to1Digits()
        if (maxMMR != null && localMMR > maxMMR) {
            "[FIELDS] ${propertyMmr.name} получено ММР $localMMR за поле $valuePropertyParticipant больше лимита $maxMMR. Устанавливаем ММР в лимит\n"
            localMMR = maxMMR
        }

        if (maxMMR != null) this.maxMMR += maxMMR
        else this.maxMMR += 2.0

        mmrValue += localMMR
        mmrValue = mmrValue.to1Digits()

        mmrEmailText += "[FIELDS] ${propertyMmr.name} текущее значение: $valuePropertyParticipant. необходимо: ${(valuePropertyMmr * mmrModificator).to1Digits()}. Вышло ММР: $localMMR\n"
    }

    private fun Double.fromDoubleValue(stock: Double): Double {
        return ((this / stock) * 100.0)
    }

    private fun Double.fromDoublePerc(stock: Double): Double {
        return when ((this / stock) * 100.0) {
            in Double.MIN_VALUE..10.0 -> 0.1
            in 10.0..30.0 -> 0.3
            in 30.0..50.0 -> 0.6
            in 50.0..70.0 -> 0.8
            in 70.0..90.0 -> 1.0
            in 90.0..110.0 -> 1.2
            in 110.0..140.0 -> 1.4
            in 140.0..180.0 -> 1.6
            in 180.0..220.0 -> 1.8
            in 220.0..260.0 -> 1.9
            in 260.0..Double.MAX_VALUE -> 2.0
            else -> 0.0
        }
    }

    override fun toString(): String {
        return "(win=${participant.win} mmr=$mmrValue, maxMMR=$maxMMR)"
    }
}