package ru.descend.bot.postgre.calculating

import ru.descend.bot.enums.EnumMMRRank
import ru.descend.bot.lolapi.LeagueMainObject.catchHeroForId
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.update
import ru.descend.bot.printLog
import ru.descend.bot.to1Digits
import kotlin.reflect.KMutableProperty1

class Calc_MMR(private var sqlData: SQLData_R2DBC, private var participant: Participants, var match: Matches, var kordlol: List<KORDLOLs>, private var mmrTable: MMRs?) {

    var mmrValue = 0.0
    private var mmrEmailText = ""
    private var mmrModificator = 1.0
    private var countFields = 0.0

    private var baseModificator = 1.3

    suspend fun init() {
        if (mmrTable != null) {
            mmrModificator = (match.matchDuration.toDouble().fromDoubleValue(mmrTable!!.matchDuration) / 100.0).to1Digits()
            if (mmrModificator < 0) {
                printLog("[CalculateMMR] mmrModificator < 0: $mmrModificator. Match: ${match.matchId}. Setting modificator 1.0")
                mmrModificator = 1.0
            }

            mmrEmailText = "МАТЧ: ${match.matchId} MODE: ${match.matchMode} ID: ${match.id} SUR: ${match.surrender} BOTS: ${match.bots}\nmmrModificator: $mmrModificator\n"

            var isFighterTank = false
            var isMageSupport = false
            catchHeroForId(participant.championId)?.let {
                if (it.tags.contains("Fighter") || it.tags.contains("Tank")) isFighterTank = true
                if (it.tags.contains("Mage") || it.tags.contains("Support")) isMageSupport = true
            }

            mmrEmailText += "isFighterTank: $isFighterTank, isMageSupport: $isMageSupport\n"

            countFields = 0.0
            calculateField(Participants::minionsKills, MMRs::minions)
            calculateField(Participants::skillsCast, MMRs::skills)

            if (isMageSupport) {
                calculateField(Participants::totalDamageShieldedOnTeammates, MMRs::shielded, minValue = 300.0)
                calculateField(Participants::totalHealsOnTeammates, MMRs::healed, minValue = 300.0)
            }

            calculateField(Participants::damageDealtToBuildings, MMRs::dmgBuilding, minValue = 500.0, maxMMR = 1.0)
            calculateField(Participants::timeCCingOthers, MMRs::controlEnemy, minValue = 3.0)

            if (!isMageSupport && isFighterTank) {
                calculateField(Participants::enemyChampionImmobilizations, MMRs::immobiliz, minValue = 5.0)
                calculateField(Participants::damageTakenOnTeamPercentage, MMRs::dmgTakenPerc)
            }
//            calculateField(Participants::skillshotsDodged, MMRs::skillDodge, minValue = 20.0, maxMMR = 1.0)

            calculateField(Participants::teamDamagePercentage, MMRs::dmgDealPerc)
            calculateField(Participants::kda, MMRs::kda)

            kordlol.find { it.LOL_id == participant.LOLperson_id }?.let {
                calculateMMRaram(it)
            }
        } else {
            printLog("MMR table is NULL")
        }
    }

    private suspend fun calculateMMRaram(kordlol: KORDLOLs?) {
        if (kordlol == null) return
        if (match.matchMode != "ARAM") return
        if (match.surrender) return
        if (match.bots) return

        mmrEmailText += "\n[BEGIN] User(kordlol_id): ${kordlol.id} Summoner: ${sqlData.getLOL(kordlol.LOL_id)?.getCorrectNameWithTag()} Champion: ${participant.championName} Win: ${participant.win}\n"
        mmrEmailText += "[BEGIN] MMR: ${kordlol.mmrAram.to1Digits()} SavedMMR: ${kordlol.mmrAramSaved.to1Digits()}\n"

        val partMMR: Double
        var newSavedMMR = calcAddSavedMMR(kordlol)

        val newAramValue: Double
        if (participant.win) {
            newAramValue = calcAddingMMR(kordlol)
            partMMR = mmrValue.to1Digits()
        } else {
            var minusMMR = calcRemoveMMR(kordlol)
            partMMR = -minusMMR.to1Digits()

            val resultMin = calcRemSavedMMR(newSavedMMR, minusMMR)
            newSavedMMR = resultMin.first
            minusMMR = resultMin.second

            newAramValue = (if (kordlol.mmrAram - minusMMR < 0.0) 0.0
            else kordlol.mmrAram - minusMMR).to1Digits()
        }

        newSavedMMR += calcRankAramMMR(kordlol.mmrAram, newAramValue)
        mmrEmailText += "\n[COMPLETED] calculateMMR: ${mmrValue.to1Digits()} mmrAram: ${newAramValue.to1Digits()} mmrAramSaved: ${newSavedMMR.to1Digits()} participant MMR: ${partMMR.to1Digits()} flat: ${mmrValue.to1Digits()}\n"

        kordlol.mmrAram = newAramValue.to1Digits()
        kordlol.mmrAramSaved = newSavedMMR.to1Digits()
        kordlol.update()

        participant.mmr = partMMR.to1Digits()
        participant.update()
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
        if (oldRank.minMMR > newRank.minMMR) {
//            value += 10.0
        }

        mmrEmailText += "[calcRankAramMMR] итого добавляем бонусных ММР: ${value.to1Digits()}\n"

        return value.to1Digits()
    }

    /**
     * Подсчет ММР которое даётся игроку (при победе)
     */
    private fun calcAddingMMR(kordlol: KORDLOLs) : Double {
        //Текущее значение ММР + новое значение ММР
        var addedValue = mmrValue.to1Digits()

        mmrEmailText += "\n[calcAddingMMR] текущее значение ММР: ${kordlol.mmrAram} Из них посчитано добавочных: $addedValue\n"

        val rank = EnumMMRRank.getMMRRank(kordlol.mmrAram)

        //В принципе победа - докидываем 1ММР сверху
        addedValue += 1.0

        //обработка штрафа к получаемому ММР в зависимости от ранга
        val removeMMR = (rank.rankValue / 10.0 * 2).to1Digits()
        if (removeMMR > 0.0) {
            mmrEmailText += "[calcAddingMMR] штраф за ранг: ${removeMMR.to1Digits()}(rankValue: ${rank.rankValue}) было ММР: $addedValue стало: ${(addedValue - removeMMR).to1Digits()}\n"
            addedValue -= removeMMR
        }

        //обработка минимума получаемых ММР
        if (addedValue < (countFields / 3.0).to1Digits()) {
            mmrEmailText += "[calcAddingMMR] получаемых ММР $addedValue меньше лимита по полям ${(countFields / 3.0).to1Digits()}(fields: $countFields). Устанавливаем в лимит\n"
            addedValue = (countFields / 3.0).to1Digits()
        }

        mmrEmailText += "[calcAddingMMR] результат получаемых ММР: ${addedValue.to1Digits()}\n"
        mmrValue = addedValue.to1Digits()

        return (kordlol.mmrAram + addedValue).to1Digits()
    }

    /**
     * Подсчет ММР которое вычитается из игрока (при поражении)
     */
    private fun calcRemoveMMR(kordlol: KORDLOLs) : Double {
        var value: Double

        val rank = EnumMMRRank.getMMRRank(kordlol.mmrAram)

        value = if (mmrValue < countFields) {
            //кол-во всех полей минус текущий ММР (сколько нехватило до Нормы)
            (countFields - mmrValue) * (1.0 + (rank.rankValue / 10.0))
        } else {
            //иначе если катали лучше Нормы - снимаем жалкую единичку
            1.0
        }.to1Digits()

        mmrEmailText += "\n[calcRemoveMMR] Начало снимания ММР: ${value.to1Digits()}\n"

        //если снимаем больше чем полей*2 - это много, органичиваем общим числом полей
        if (value > countFields) {
            mmrEmailText += "[calcRemoveMMR] попытка снять ${value.to1Digits()} больше чем кол-во полей($countFields) Снимаем ММР $countFields\n"
            value = countFields
        }

        //лимит на максимальное снятие
        val maxRemoved = (rank.rankValue + 1.0).to1Digits()
        if (value > maxRemoved){
            mmrEmailText += "[calcRemoveMMR] попытка снять больше ММР(${value.to1Digits()} чем максимальный лимит по рангу: $maxRemoved (rankValue:${rank.rankValue} + 1). Снимаем лимит.\n"
            value = maxRemoved
        }

        //лимит на минимальное снятие
        val minRemoved = (1 + rank.rankValue * 0.5).to1Digits()
        if (value < minRemoved) {
            mmrEmailText += "[calcRemoveMMR] попытка снять меньше ММР(${value.to1Digits()} чем минимальный лимит по рангу: $minRemoved (rankValue:${rank.rankValue} * 0.5). Снимаем лимит.\n"
            value = minRemoved
        }

        mmrEmailText += "[calcRemoveMMR] итого снимаем ММР: ${value.to1Digits()}\n"

        return value.to1Digits()
    }

    /**
     * Подсчет увеличения бонусных ММР
     */
    private fun calcAddSavedMMR(kordlol: KORDLOLs) : Double {
        val value: Double

        //текущее значение бонусных ММР
        val newSavedMMR = kordlol.mmrAramSaved.to1Digits()

        val rank = EnumMMRRank.getMMRRank(kordlol.mmrAram)

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
            in Double.MIN_VALUE..10.0 -> 0.0
            in 10.0..30.0 -> 0.2
            in 30.0..50.0 -> 0.4
            in 50.0..70.0 -> 0.6
            in 70.0..90.0 -> 0.8
            in 90.0..110.0 -> 1.0
            in 110.0..140.0 -> 1.2
            in 140.0..180.0 -> 1.4
            in 180.0..220.0 -> 1.6
            in 220.0..260.0 -> 1.8
            in 260.0..Double.MAX_VALUE -> 2.0
            else -> 0.0
        }
    }

    override fun toString(): String {
        return "MMR(win=${participant.win} mmr=$mmrValue, fields=$countFields)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Calc_MMR

        if (sqlData != other.sqlData) return false
        if (participant != other.participant) return false
        if (match != other.match) return false
        if (kordlol != other.kordlol) return false
        if (mmrTable != other.mmrTable) return false
        if (mmrValue != other.mmrValue) return false
        if (mmrEmailText != other.mmrEmailText) return false
        if (mmrModificator != other.mmrModificator) return false
        if (countFields != other.countFields) return false
        if (baseModificator != other.baseModificator) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sqlData.hashCode()
        result = 31 * result + participant.hashCode()
        result = 31 * result + match.hashCode()
        result = 31 * result + kordlol.hashCode()
        result = 31 * result + (mmrTable?.hashCode() ?: 0)
        result = 31 * result + mmrValue.hashCode()
        result = 31 * result + mmrEmailText.hashCode()
        result = 31 * result + mmrModificator.hashCode()
        result = 31 * result + countFields.hashCode()
        result = 31 * result + baseModificator.hashCode()
        return result
    }


}