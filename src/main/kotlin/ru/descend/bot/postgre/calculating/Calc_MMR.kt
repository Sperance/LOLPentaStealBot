package ru.descend.bot.postgre.calculating

import ru.descend.bot.enums.EnumMMRRank
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.printLog
import ru.descend.bot.to1Digits
import kotlin.math.max
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1

class Calc_MMR(private var participant: ParticipantsNew, var match: Matches, var lols: LOLs, private var mmrTable: MMRs?) {

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

            calculateField(ParticipantsNew::totalMinionsKilled, MMRs::minions)
            calculateField(ParticipantsNew::abilityUses, MMRs::skills, maxMMR = 1.0)

            calculateField(ParticipantsNew::totalDamageShieldedOnTeammates, MMRs::shielded, maxMMR = 2.0)
            calculateField(ParticipantsNew::totalHealsOnTeammates, MMRs::healed)

            calculateField(ParticipantsNew::damageDealtToBuildings, MMRs::dmgBuilding, maxMMR = 2.0)
            calculateField(ParticipantsNew::timeCCingOthers, MMRs::controlEnemy, maxMMR = 2.0)

            calculateField(ParticipantsNew::enemyChampionImmobilizations, MMRs::immobiliz, maxMMR = 2.0)
            calculateField(ParticipantsNew::damageTakenOnTeamPercentage, MMRs::dmgTakenPerc)
            calculateField(ParticipantsNew::skillshotsDodged, MMRs::skillDodge, maxMMR = 2.0)

            calculateField(ParticipantsNew::teamDamagePercentage, MMRs::dmgDealPerc)
            calculateField(ParticipantsNew::kda, MMRs::kda)

            calculateMMRaram(lols)
        }
    }

    private fun calculateMMRaram(lols: LOLs) {
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
            (tempMaxMMR - mmrValue) * (0.2 + rank.rankValue / 10.0)
        } else {
            1.0
        }.to1Digits()

        if (value > tempMaxMMR) value = tempMaxMMR
        if (value < 1) value = 1.0

        stockGainMMR = -value.to1Digits()
    }

    private fun calculateField(propertyParticipant: KMutableProperty1<ParticipantsNew, *>, propertyMmr: KMutableProperty1<MMRs, *>, maxMMR: Double? = null) {
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
            in Double.MIN_VALUE..20.0 -> 0.2
            in 20.0..40.0 -> 0.4
            in 40.0..60.0 -> 0.6
            in 60.0..80.0 -> 0.8
            in 80.0..100.0 -> 1.0
            in 100.0..120.0 -> 1.2
            in 120.0..140.0 -> 1.4
            in 140.0..160.0 -> 1.6
            in 160.0..180.0 -> 1.8
            in 180.0..200.0 -> 2.0
            in 200.0..220.0 -> 2.2
            in 220.0..240.0 -> 2.4
            in 240.0..260.0 -> 2.6
            in 260.0..280.0 -> 2.8
            in 280.0..Double.MAX_VALUE -> 3.0
            else -> 0.0
        }
    }

    override fun toString(): String {
        return "(win=${participant.win} mmr=$mmrValue, maxMMR=$maxMMR)"
    }
}