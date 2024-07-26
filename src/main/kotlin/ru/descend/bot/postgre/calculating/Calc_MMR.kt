package ru.descend.bot.postgre.calculating

import ru.descend.bot.ADD_MMR_FOR_LOOSE_ARAM_CALC
import ru.descend.bot.MMR_STOCK_MODIFICATOR
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.printLog
import ru.descend.bot.to1Digits
import kotlin.reflect.KMutableProperty1

class Calc_MMR(private var participant: Collection<ParticipantsNew>, val match: Matches) {

    var mmrValue = 0.0
    var mmrValueTextLog = ""
    private var mmrMatchModificator = 1.0
    private var stockGainMMR = 0.0
    private var maxMMRforLoose: Double = 0.0

    suspend fun calculateMMR() {
        participant.filter { it.win }.forEach { par ->
            val mmrTable = R2DBC.getMMRforChampion(par.championName)
            if (mmrTable != null) {
                calculateSingleParticipant(par, mmrTable)
            }
        }
        participant.filter { !it.win }.forEach { par ->
            val mmrTable = R2DBC.getMMRforChampion(par.championName)
            if (mmrTable != null) {
                calculateSingleParticipant(par, mmrTable)
            }
        }
    }

    private suspend fun calculateSingleParticipant(par: ParticipantsNew, mmrTable: MMRs) {

        stockGainMMR = 0.0
        mmrValue = 0.0

        mmrMatchModificator = (match.matchDuration.toDouble() / mmrTable.matchDuration).to1Digits()
        if (mmrMatchModificator < 0) {
            mmrMatchModificator = 1.0
        }

        mmrValueTextLog += "\nMMR for ${par.riotIdGameName}#${par.riotIdTagline} WIN: ${par.win} Champion: ${par.championName} maxMMRforLoose: $maxMMRforLoose\n\n"

        calculateField(par, ParticipantsNew::totalMinionsKilled, mmrTable, MMRs::minions, maxMMR = 5.0)
        calculateField(par, ParticipantsNew::abilityUses, mmrTable, MMRs::skills, maxMMR = 2.0)

        calculateField(par, ParticipantsNew::totalDamageShieldedOnTeammates, mmrTable, MMRs::shielded, maxMMR = 5.0)
        calculateField(par, ParticipantsNew::totalHealsOnTeammates, mmrTable, MMRs::healed, maxMMR = 5.0)

//        calculateField(par, ParticipantsNew::damageDealtToBuildings, mmrTable, MMRs::dmgBuilding, maxMMR = 2.0)
        calculateField(par, ParticipantsNew::timeCCingOthers, mmrTable, MMRs::controlEnemy, maxMMR = 2.0)

        calculateField(par, ParticipantsNew::enemyChampionImmobilizations, mmrTable, MMRs::immobiliz, maxMMR = 2.0)
        calculateField(par, ParticipantsNew::damageTakenOnTeamPercentage, mmrTable, MMRs::dmgTakenPerc, maxMMR = 5.0)
//            calculateField(par, ParticipantsNew::skillshotsDodged, MMRs::skillDodge, maxMMR = 2.0)

        calculateField(par, ParticipantsNew::teamDamagePercentage, mmrTable, MMRs::dmgDealPerc, maxMMR = 5.0)
        calculateField(par, ParticipantsNew::kda, mmrTable, MMRs::kda, maxMMR = 5.0)

        calculateMMRaram(par)

        mmrValueTextLog += "\nstockGainMMR:$stockGainMMR, mmrValue:$mmrValue, mmrMatchModificator:$mmrMatchModificator\n"
    }

    private fun calculateAdditionalFields(par: ParticipantsNew) {
        //Топ урона
        if (par.highestChampionDamage > 0) {
            mmrValue += 2.0
            mmrValueTextLog += "\t[VALUE] highestChampionDamage: ${par.highestChampionDamage} result MMR: $mmrValue\n"
        }
        //Топ контроля
        if (par.highestCrowdControlScore > 0) {
            mmrValue += 2.0
            mmrValueTextLog += "\t[VALUE] highestChampionDamage: ${par.highestCrowdControlScore} result MMR: $mmrValue\n"
        }
    }

    private suspend fun calculateMMRaram(par: ParticipantsNew) {
        calculateAdditionalFields(par)

        if (par.win) {
            stockGainMMR = mmrValue.to1Digits()
        } else {
            calcRemoveMMR(par)
        }

        if (stockGainMMR > maxMMRforLoose) maxMMRforLoose = stockGainMMR

        par.gameMatchMmr = stockGainMMR.to1Digits()
        par.gameMatchKey = ""
    }

    /**
     * Подсчет ММР которое вычитается из игрока (при поражении)
     */
    private suspend fun calcRemoveMMR(par: ParticipantsNew) {
        var value: Double
        val tempMaxMMR = maxMMRforLoose + ADD_MMR_FOR_LOOSE_ARAM_CALC

        val lolObj = par.LOLpersonObj()
        var mod = if (lolObj != null) 0.8 + (lolObj.getRank().rankValue / 10.0) else 1.0

        value = if (tempMaxMMR > mmrValue) {
            (tempMaxMMR - mmrValue) * mod
        } else {
            1.0
        }.to1Digits()

        if (value > tempMaxMMR) value = tempMaxMMR
        if (value < 1) value = 1.0

        stockGainMMR = -value.to1Digits()
    }

    private fun calculateField(par: ParticipantsNew, propertyParticipant: KMutableProperty1<ParticipantsNew, *>, mmrTable: MMRs,  propertyMmr: KMutableProperty1<MMRs, *>, maxMMR: Double) {

        val valuePropertyMmr = ((propertyMmr.invoke(mmrTable) as Double) * MMR_STOCK_MODIFICATOR).to1Digits()
        val valuePropertyParticipant = when (val valuePart = propertyParticipant.invoke(par)){
            is Int -> valuePart.toDouble()
            is Double -> valuePart
            is Long -> valuePart.toDouble()
            else -> {
                printLog("[calculateField] Fail convert field Participant ${propertyParticipant.name} to Double: $valuePart")
                0.0
            }
        }.to1Digits()

        var localMMR = (valuePropertyParticipant / (valuePropertyMmr * mmrMatchModificator)).to1Digits()
        if (localMMR > maxMMR) {
            localMMR = maxMMR.to1Digits()
        }

        if (valuePropertyParticipant < 0.01) {
            mmrValueTextLog += "\t[FIELD:skipped] ${propertyParticipant.name} current: $valuePropertyParticipant stock: ${(valuePropertyMmr * mmrMatchModificator).to1Digits()} mmr gained: $localMMR maxMMR: $maxMMR\n"
            return
        }

        mmrValueTextLog += "\t[FIELD] ${propertyParticipant.name} current: $valuePropertyParticipant stock: ${(valuePropertyMmr * mmrMatchModificator).to1Digits()} mmr gained: $localMMR maxMMR: $maxMMR\n"

        mmrValue = (localMMR + mmrValue).to1Digits()
    }
}