package ru.descend.bot.postgre.calculating

import ru.descend.bot.MMR_STOCK_MODIFICATOR
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.printLog
import ru.descend.bot.to1Digits
import kotlin.reflect.KMutableProperty1

class Calc_MMR(private var participant: ParticipantsNew, private var match: Matches, private var mmrTable: MMRs?) {

    var mmrValue = 0.0
    var mmrValueTextLog = ""
    private var mmrMatchModificator = 1.0
    private var stockGainMMR = 0.0
    private var maxMMR = 0.0

    fun init() {
        if (mmrTable != null) {
            mmrMatchModificator = (match.matchDuration.toDouble() / mmrTable!!.matchDuration).to1Digits()
            if (mmrMatchModificator < 0) {
                mmrMatchModificator = 1.0
            }

            mmrValueTextLog = "MMR for ${participant.riotIdGameName}#${participant.riotIdTagline} WIN: ${participant.win} Champion: ${participant.championName}\n\n"

            calculateField(ParticipantsNew::totalMinionsKilled, MMRs::minions, maxMMR = 5.0)
            calculateField(ParticipantsNew::abilityUses, MMRs::skills, maxMMR = 2.0)

            calculateField(ParticipantsNew::totalDamageShieldedOnTeammates, MMRs::shielded, maxMMR = 5.0)
            calculateField(ParticipantsNew::totalHealsOnTeammates, MMRs::healed, maxMMR = 5.0)

            calculateField(ParticipantsNew::damageDealtToBuildings, MMRs::dmgBuilding, maxMMR = 2.0)
            calculateField(ParticipantsNew::timeCCingOthers, MMRs::controlEnemy, maxMMR = 2.0)

            calculateField(ParticipantsNew::enemyChampionImmobilizations, MMRs::immobiliz, maxMMR = 2.0)
            calculateField(ParticipantsNew::damageTakenOnTeamPercentage, MMRs::dmgTakenPerc, maxMMR = 5.0)
//            calculateField(ParticipantsNew::skillshotsDodged, MMRs::skillDodge, maxMMR = 2.0)

            calculateField(ParticipantsNew::teamDamagePercentage, MMRs::dmgDealPerc, maxMMR = 5.0)
            calculateField(ParticipantsNew::kda, MMRs::kda, maxMMR = 5.0)

            calculateMMRaram()

            mmrValueTextLog += "\nstockGainMMR:$stockGainMMR, maxMMR:$maxMMR, mmrValue:$mmrValue, mmrMatchModificator:$mmrMatchModificator\n"
        }
    }

    private fun calculateMMRaram() {
        if (participant.win) {
            stockGainMMR = mmrValue.to1Digits()
        } else {
            calcRemoveMMR()
        }

        participant.gameMatchMmr = stockGainMMR.to1Digits()
        participant.gameMatchKey = ""
    }

    /**
     * Подсчет ММР которое вычитается из игрока (при поражении)
     */
    private fun calcRemoveMMR() {
        var value: Double
        val tempMaxMMR = maxMMR

        value = if (tempMaxMMR > mmrValue) {
            (tempMaxMMR - mmrValue) * 0.5
        } else {
            1.0
        }.to1Digits()

        if (value > tempMaxMMR) value = tempMaxMMR
        if (value < 1) value = 1.0

        stockGainMMR = -value.to1Digits()
    }

    private fun calculateField(propertyParticipant: KMutableProperty1<ParticipantsNew, *>, propertyMmr: KMutableProperty1<MMRs, *>, maxMMR: Double? = null) {
        if (mmrTable == null) return

        val valuePropertyMmr = ((propertyMmr.invoke(mmrTable!!) as Double) * MMR_STOCK_MODIFICATOR).to1Digits()
        val valuePropertyParticipant = when (val valuePart = propertyParticipant.invoke(participant)){
            is Int -> valuePart.toDouble()
            is Double -> valuePart
            is Long -> valuePart.toDouble()
            else -> {
                printLog("[calculateField] Fail convert field Participant ${propertyParticipant.name} to Double: $valuePart")
                0.0
            }
        }.to1Digits()

        var localMMR = (valuePropertyParticipant / (valuePropertyMmr * mmrMatchModificator)).to1Digits()
        if (maxMMR != null && localMMR > maxMMR) {
            localMMR = maxMMR.to1Digits()
        }

        if (valuePropertyParticipant < 0.01) {
            mmrValueTextLog += "\t[FIELD:skipped] ${propertyParticipant.name} current: $valuePropertyParticipant stock: ${(valuePropertyMmr * mmrMatchModificator).to1Digits()} mmr gained: $localMMR maxMMR: $maxMMR\n"
            return
        }

        mmrValueTextLog += "\t[FIELD] ${propertyParticipant.name} current: $valuePropertyParticipant stock: ${(valuePropertyMmr * mmrMatchModificator).to1Digits()} mmr gained: $localMMR maxMMR: $maxMMR\n"

        if (maxMMR != null) this.maxMMR += maxMMR
        else this.maxMMR += 2.0

        mmrValue = (localMMR + mmrValue).to1Digits()
    }

    override fun toString(): String {
        return "(win=${participant.win} mmr=$mmrValue, maxMMR=$maxMMR)"
    }
}