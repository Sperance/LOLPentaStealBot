package ru.descend.bot.postgre.calculating

import ru.descend.bot.enums.EnumMMRRank
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.printLog
import ru.descend.bot.to1Digits
import kotlin.reflect.KMutableProperty1

class Calc_MMR(private var participant: Participants, var match: Matches, var lols: LOLs, private var mmrTable: MMRs?) {

    var mmrValue = 0.0
    private var mmrEmailText = ""
    private var mmrModificator = 1.0
    private var countFields = 0.0

    private var baseModificator = 1.3

    fun getSavedParticipant() = participant
    fun getSavedLOL() = lols

    fun init() {
        if (mmrTable != null) {
            mmrModificator = (match.matchDuration.toDouble().fromDoubleValue(mmrTable!!.matchDuration) / 100.0).to1Digits()
            if (mmrModificator < 0) {
                printLog("[CalculateMMR] mmrModificator < 0: $mmrModificator. Match: ${match.matchId}. Setting modificator 1.0")
                mmrModificator = 1.0
            }

            mmrEmailText = "МАТЧ: ${match.matchId} MODE: ${match.matchMode} ID: ${match.id} SUR: ${match.surrender} BOTS: ${match.bots}\nmmrModificator: $mmrModificator\n"

            calculateField(Participants::minionsKills, MMRs::minions)
            calculateField(Participants::skillsCast, MMRs::skills)

            calculateField(Participants::totalDamageShieldedOnTeammates, MMRs::shielded, minValue = 300.0, maxMMR = 1.0)
            calculateField(Participants::totalHealsOnTeammates, MMRs::healed, minValue = 300.0, maxMMR = 1.0)

            calculateField(Participants::damageDealtToBuildings, MMRs::dmgBuilding, minValue = 500.0, maxMMR = 1.0)
            calculateField(Participants::timeCCingOthers, MMRs::controlEnemy, minValue = 3.0)

            calculateField(Participants::enemyChampionImmobilizations, MMRs::immobiliz, minValue = 5.0, maxMMR = 1.0)
            calculateField(Participants::damageTakenOnTeamPercentage, MMRs::dmgTakenPerc, maxMMR = 1.0)
            calculateField(Participants::skillshotsDodged, MMRs::skillDodge, minValue = 20.0, maxMMR = 1.0)

            calculateField(Participants::teamDamagePercentage, MMRs::dmgDealPerc)
            calculateField(Participants::kda, MMRs::kda)

            calculateMMRaram(lols)
        }
    }

    private fun calculateMMRaram(lols: LOLs) {
        if (match.surrender) return
        if (match.bots) return

        val partMMR: Double
        var newSavedMMR = calcAddSavedMMR(lols)

        val newAramValue: Double
        if (participant.win) {
            newAramValue = calcAddingMMR(lols)
            partMMR = mmrValue.to1Digits()
        } else {
            var minusMMR = calcRemoveMMR(lols)
            partMMR = -minusMMR.to1Digits()

            val resultMin = calcRemSavedMMR(newSavedMMR, minusMMR)
            newSavedMMR = resultMin.first
            minusMMR = resultMin.second

            newAramValue = (if (lols.mmrAram - minusMMR < 0.0) 0.0
            else lols.mmrAram - minusMMR).to1Digits()
        }

        newSavedMMR += calcRankAramMMR(lols.mmrAram, newAramValue)
        mmrEmailText += "\n[COMPLETED] calculateMMR: ${mmrValue.to1Digits()} mmrAram: ${newAramValue.to1Digits()} mmrAramSaved: ${newSavedMMR.to1Digits()} participant MMR: ${partMMR.to1Digits()} flat: ${mmrValue.to1Digits()}\n"

        lols.mmrAram = newAramValue.to1Digits()
        lols.mmrAramSaved = newSavedMMR.to1Digits()
        lols.mmrAramLast = partMMR.to1Digits()
        lols.mmrAramLastChampion = participant.championId
    }

    /**
     * Изменение бонусных ММР за счёт рангов Арам
     */
    private fun calcRankAramMMR(oldMMR: Double, newMMR: Double) : Double {
        var value = 0.0

        val oldRank = EnumMMRRank.getMMRRank(oldMMR)
        val newRank = EnumMMRRank.getMMRRank(newMMR)

        if (oldRank.minMMR == newRank.minMMR) return value.to1Digits()

        mmrEmailText += "\n[calcRankAramMMR] прошлый ранг: ${oldRank.nameRank}(ММР: ${oldRank.minMMR}) новый ранг: ${newRank.nameRank}(ММР: ${newRank.minMMR})\n"

        //Ранг повышен
        if (oldRank.minMMR < newRank.minMMR) {
            value += 10.0
        }

        //Ранг понижен
//        if (oldRank.minMMR > newRank.minMMR) {
//            value += 10.0
//        }

        mmrEmailText += "[calcRankAramMMR] итого добавляем бонусных ММР: ${value.to1Digits()}\n"

        return value.to1Digits()
    }

    /**
     * Подсчет ММР которое даётся игроку (при победе)
     */
    private fun calcAddingMMR(lols: LOLs) : Double {
        //Текущее значение ММР + новое значение ММР
        var addedValue = mmrValue.to1Digits()

        val rank = EnumMMRRank.getMMRRank(lols.mmrAram)

        //В принципе победа - докидываем 1ММР сверху
//        addedValue += 1.0

        //обработка штрафа к получаемому ММР в зависимости от ранга
        val removeMMR = (rank.rankValue / 3.0).to1Digits()
        if (removeMMR > 0.0) {
            addedValue -= removeMMR
        }

        //обработка минимума получаемых ММР
        if (addedValue < 1.0) {
            addedValue = 1.0
        }

        mmrValue = addedValue.to1Digits()

        return (lols.mmrAram + addedValue).to1Digits()
    }

    /**
     * Подсчет ММР которое вычитается из игрока (при поражении)
     */
    private fun calcRemoveMMR(lols: LOLs) : Double {
        var value: Double

        val rank = EnumMMRRank.getMMRRank(lols.mmrAram)

        value = if (mmrValue < countFields) {
            //кол-во всех полей минус текущий ММР (сколько нехватило до Нормы)
            (countFields - mmrValue) * (1.0 + (rank.rankValue / 5.0))
        } else {
            //иначе если катали лучше Нормы - снимаем жалкую единичку
            1.0
        }.to1Digits()

        //лимит на минимальное снятие
        val minRemoved = (rank.rankValue * 0.5).to1Digits()
        if (value < minRemoved) {
            value = minRemoved
        }

        //лимит на максимальное снятие
        val maxRemoved = (rank.rankValue + 1.0).to1Digits()
        if (value > maxRemoved){
            value = maxRemoved
        }

        return value.to1Digits()
    }

    /**
     * Подсчет увеличения бонусных ММР
     */
    private fun calcAddSavedMMR(lols: LOLs) : Double {
        val value: Double

        //текущее значение бонусных ММР
        val newSavedMMR = lols.mmrAramSaved.to1Digits()

        val rank = EnumMMRRank.getMMRRank(lols.mmrAram)

        //лимит получаемых бонусных ММР за матч
        val limitMMR = rank.rankValue + 5.0

        mmrEmailText += "\n[calcAddSavedMMR] текущее значение бонус ММР: $newSavedMMR\n"

        //подсчет добавочных бонусных ММР
        var addSavedMMR = 0.0

        //за каждую пенту 5 очков
        if (participant.kills5 > 0) {
            mmrEmailText += "[calcAddSavedMMR] сделано Пент: ${participant.kills5}\n"
            addSavedMMR += participant.kills5 * 5.0
        }

        //за каждую квадру 2 очка
        if (participant.kills4 > 0) {
            mmrEmailText += "[calcAddSavedMMR] сделано Квадр: ${participant.kills4}\n"
            addSavedMMR += participant.kills4 * 2.0
        }

        if (participant.win) {
            mmrEmailText += "[calcAddSavedMMR] из-за Победы зачисляем 0.5 ММР бонуса\n"
            addSavedMMR += 0.5
        }

        if (addSavedMMR > limitMMR) {
            mmrEmailText += "[calcAddSavedMMR] попытка получить больше ММР $addSavedMMR чем лимит $limitMMR\n"
            addSavedMMR = limitMMR
        }

        value = newSavedMMR + addSavedMMR

        mmrEmailText += "[calcAddSavedMMR] Добавляем бонусные ММР: ${addSavedMMR.to1Digits()}\n"

        return value.to1Digits()
    }

    /**
     * Вычитаем ММР из бонусных
     */
    private fun calcRemSavedMMR(_newSavedMMR: Double, _minusMMR: Double) : Pair<Double, Double> {

        var newSavedMMR = _newSavedMMR
        var minusMMR = _minusMMR

        mmrEmailText += "\n[calcRemSavedMMR] бонусных ММР получено ранее: $newSavedMMR, вычитаемых ММР: $minusMMR\n"

        //если бонусных ММР больше или равно вычитаемых, всё просто
        if (newSavedMMR >= minusMMR){
            mmrEmailText += "[calcRemSavedMMR] бонусных ММР $newSavedMMR больше либо равно чем вычитаем: $minusMMR. Вычитаем 0. Осталось бонусных: ${(newSavedMMR - minusMMR).to1Digits()}\n"
            newSavedMMR -= minusMMR
            minusMMR = 0.0
        } else {
            mmrEmailText += "[calcRemSavedMMR] бонусных ММР $newSavedMMR меньше вычитаемых ММР: $minusMMR. Бонусных 0. Вычитаем всего ${(minusMMR - newSavedMMR).to1Digits()}\n"
            minusMMR -= newSavedMMR
            newSavedMMR = 0.0
        }

        mmrEmailText += "[calcRemSavedMMR] результат бонусных ММР: ${newSavedMMR.to1Digits()} вычитаемых ММР: ${minusMMR.to1Digits()}\n"

        return Pair(newSavedMMR, minusMMR)
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
        countFields++

        var localMMR = valuePropertyParticipant.fromDoublePerc(valuePropertyMmr * mmrModificator).to1Digits()
        if (maxMMR != null && localMMR > maxMMR) {
            "[FIELDS] ${propertyMmr.name} получено ММР $localMMR за поле $valuePropertyParticipant больше лимита $maxMMR. Устанавливаем ММР в лимит\n"
            localMMR = maxMMR
        }

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
            in 220.0..260.0 -> 2.0
            in 260.0..Double.MAX_VALUE -> 2.5
            else -> 0.0
        }
    }

    override fun toString(): String {
        return "(win=${participant.win} mmr=$mmrValue, fields=$countFields)"
    }
}