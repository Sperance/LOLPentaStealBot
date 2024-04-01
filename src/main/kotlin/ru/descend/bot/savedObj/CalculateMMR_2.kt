package ru.descend.bot.savedObj

import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.printLog
import ru.descend.bot.to2Digits
import kotlin.math.abs
import kotlin.reflect.KMutableProperty1

private const val modRank = 70.0
private const val modTitle = 100.0

enum class EnumMMRRank(val nameRank: String, val minMMR: Double, val rankValue: Int) {
    UNRANKED("Нет ранга", 0.0, 0),
    PAPER_III("Бумага III", UNRANKED.minMMR + modRank, 1),
    PAPER_II("Бумага II", PAPER_III.minMMR + modRank, 1),
    PAPER_I("Бумага I", PAPER_II.minMMR + modRank, 1),
    WOOD_III("Дерево III", PAPER_I.minMMR + modTitle, 2),
    WOOD_II("Дерево II", WOOD_III.minMMR + modRank, 2),
    WOOD_I("Дерево I", WOOD_II.minMMR + modRank, 2),
    IRON_III("Железо III", WOOD_I.minMMR + modTitle, 3),
    IRON_II("Железо II", IRON_III.minMMR + modRank, 3),
    IRON_I("Железо I", IRON_II.minMMR + modRank, 3),
    BRONZE_III("Бронза III", IRON_I.minMMR + modTitle, 4),
    BRONZE_II("Бронза II", BRONZE_III.minMMR + modRank, 4),
    BRONZE_I("Бронза I", BRONZE_II.minMMR + modRank, 4),
    SILVER_III("Серебро III", BRONZE_I.minMMR + modTitle, 5),
    SILVER_II("Серебро II", SILVER_III.minMMR + modRank, 5),
    SILVER_I("Серебро I", SILVER_II.minMMR + modRank, 5),
    GOLD_III("Золото III", SILVER_I.minMMR + modTitle, 6),
    GOLD_II("Золото II", GOLD_III.minMMR + modRank, 6),
    GOLD_I("Золото I", GOLD_II.minMMR + modRank, 6),
    PLATINUM_III("Платина III", GOLD_I.minMMR + modTitle, 7),
    PLATINUM_II("Платина II", PLATINUM_III.minMMR + modRank, 7),
    PLATINUM_I("Платина I", PLATINUM_II.minMMR + modRank, 7),
    DIAMOND_III("Алмаз III", PLATINUM_I.minMMR + modTitle, 8),
    DIAMOND_II("Алмаз II", DIAMOND_III.minMMR + modRank, 8),
    DIAMOND_I("Алмаз I", DIAMOND_II.minMMR + modRank, 8),
    MASTER_III("Мастер III", DIAMOND_I.minMMR + modTitle, 9),
    MASTER_II("Мастер II", MASTER_III.minMMR + modRank, 9),
    MASTER_I("Мастер I", MASTER_II.minMMR + modRank, 9),
    CHALLENGER("Челленджер", MASTER_I.minMMR + modTitle, 10)
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

class CalculateMMR_2(private var sqlData: SQLData_R2DBC, private var participant: Participants, var match: Matches, var kordlol: List<KORDLOLs>, private var mmrTable: MMRs?) {

    private var mmrValue = 0.0
    private var mmrEmailText = ""
    private var mmrModificator = 1.0
    private var countFields = 0.0

    private var baseModificator = 1.3

    suspend fun init() {
        if (mmrTable != null) {
            mmrModificator = (match.matchDuration.toDouble().fromDoubleValue(mmrTable!!.matchDuration) / 100.0).to2Digits()
            if (mmrModificator < 0) {
                printLog("[CalculateMMR] mmrModificator < 0: $mmrModificator. Match: ${match.matchId}. Setting modificator 1.0")
                mmrModificator = 1.0
            }

            mmrEmailText = "МАТЧ: ${match.matchId} MODE: ${match.matchMode} ID: ${match.id} SUR: ${match.surrender} BOTS: ${match.bots}\nmmrModificator: $mmrModificator\n"

            var isFighterTank = false
            var isMageSupport = false
            LeagueMainObject.catchHeroForId(participant.championId.toString())?.let {
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
            if (match.matchMode == "ARAM") {
                sqlData.sendEmail("${match.matchId} (${match.id})", mmrEmailText)
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

        mmrEmailText += "\n[BEGIN] User(kordlol_id): ${kordlol.id} Summoner: ${sqlData.getLOL(kordlol.LOL_id)?.LOL_summonerName} Champion: ${participant.championName} Win: ${participant.win}\n"
        mmrEmailText += "[BEGIN] MMR: ${kordlol.mmrAram.to2Digits()} SavedMMR: ${kordlol.mmrAramSaved.to2Digits()}\n"

        val partMMR: Double
        var newSavedMMR = calcAddSavedMMR(kordlol)

        val newAramValue: Double
        if (participant.win) {
            newAramValue = calcAddingMMR(kordlol)
            partMMR = mmrValue.to2Digits()
        } else {
            var minusMMR = calcRemoveMMR(kordlol)
            partMMR = -minusMMR.to2Digits()

            val resultMin = calcRemSavedMMR(newSavedMMR, minusMMR)
            newSavedMMR = resultMin.first
            minusMMR = resultMin.second

            newAramValue = (if (kordlol.mmrAram - minusMMR < 0.0) 0.0
            else kordlol.mmrAram - minusMMR).to2Digits()
        }

        newSavedMMR += calcRankAramMMR(kordlol.mmrAram, newAramValue)
        mmrEmailText += "\n[COMPLETED] calculateMMR: ${mmrValue.to2Digits()} mmrAram: ${newAramValue.to2Digits()} mmrAramSaved: ${newSavedMMR.to2Digits()} participant MMR: ${partMMR.to2Digits()} flat: ${mmrValue.to2Digits()}\n"

        kordlol.mmrAram = newAramValue.to2Digits()
        kordlol.mmrAramSaved = newSavedMMR.to2Digits()
        kordlol.update()

        participant.mmr = partMMR.to2Digits()
        participant.mmrFlat = mmrValue.to2Digits()
        participant.update()
    }

    /**
     * Изменение бонусных ММР за счёт рангов Арам
     */
    private fun calcRankAramMMR(oldMMR: Double, newMMR: Double) : Double {
        var value = 0.0

        val oldRank = EnumMMRRank.getMMRRank(oldMMR)
        val newRank = EnumMMRRank.getMMRRank(newMMR)

        if (oldRank.minMMR == newRank.minMMR) return value.to2Digits()

        mmrEmailText += "\n[calcRankAramMMR] прошлый ранг: ${oldRank.nameRank}(ММР: ${oldRank.minMMR}) новый ранг: ${newRank.nameRank}(ММР: ${newRank.minMMR})\n"

        //Ранг повышен
        if (oldRank.minMMR < newRank.minMMR) {
            value += 10.0
        }

        //Ранг понижен
        if (oldRank.minMMR > newRank.minMMR) {
//            value += 10.0
        }

        mmrEmailText += "[calcRankAramMMR] итого добавляем бонусных ММР: ${value.to2Digits()}\n"

        return value.to2Digits()
    }

    /**
     * Подсчет ММР которое даётся игроку (при победе)
     */
    private fun calcAddingMMR(kordlol: KORDLOLs) : Double {
        //Текущее значение ММР + новое значение ММР
        var addedValue = mmrValue.to2Digits()

        mmrEmailText += "\n[calcAddingMMR] текущее значение ММР: ${kordlol.mmrAram} Из них посчитано добавочных: $addedValue\n"

        val rank = EnumMMRRank.getMMRRank(kordlol.mmrAram)

        //обработка штрафа к получаемому ММР в зависимости от ранга
        val removeMMR = (rank.rankValue / 10.0 * 2).to2Digits()
        if (removeMMR > 0.0) {
            mmrEmailText += "[calcAddingMMR] штраф за ранг: ${removeMMR.to2Digits()}(rankValue: ${rank.rankValue}) было ММР: $addedValue стало: ${(addedValue - removeMMR).to2Digits()}\n"
            addedValue -= removeMMR
        }

        //обработка минимума получаемых ММР
        if (addedValue < (countFields / 3.0).to2Digits()) {
            mmrEmailText += "[calcAddingMMR] получаемых ММР $addedValue меньше лимита по полям ${(countFields / 3.0).to2Digits()}(fields: $countFields). Устанавливаем в лимит\n"
            addedValue = (countFields / 3.0).to2Digits()
        }

        mmrEmailText += "[calcAddingMMR] результат получаемых ММР: ${addedValue.to2Digits()}\n"
        mmrValue = addedValue.to2Digits()

        return (kordlol.mmrAram + addedValue).to2Digits()
    }

    /**
     * Подсчет ММР которое вычитается из игрока (при поражении)
     */
    private fun calcRemoveMMR(kordlol: KORDLOLs) : Double {
        var value: Double

        value = if (mmrValue < countFields) {
            //кол-во всех полей минус текущий ММР (сколько нехватило до Нормы)
            (countFields - mmrValue) * 1.5
        } else {
            //иначе если катали лучше Нормы - снимаем жалкую единичку
            1.0
        }.to2Digits()

        mmrEmailText += "\n[calcRemoveMMR] Начало снимания ММР: ${value.to2Digits()}\n"

        //если снимаем больше чем полей*2 - это много, органичиваем общим числом полей
        if (value > countFields) {
            mmrEmailText += "[calcRemoveMMR] попытка снять ${value.to2Digits()} больше чем кол-во полей($countFields) Снимаем ММР $countFields\n"
            value = countFields
        }


        val rank = EnumMMRRank.getMMRRank(kordlol.mmrAram)

        //лимит на максимальное снятие
        val maxRemoved = (rank.rankValue + 1.0).to2Digits()
        if (value > maxRemoved){
            mmrEmailText += "[calcRemoveMMR] попытка снять больше ММР(${value.to2Digits()} чем максимальный лимит по рангу: $maxRemoved (rankValue:${rank.rankValue} + 1). Снимаем лимит.\n"
            value = maxRemoved
        }

        //лимит на минимальное снятие
        val minRemoved = (rank.rankValue * 0.5).to2Digits()
        if (value < minRemoved) {
            mmrEmailText += "[calcRemoveMMR] попытка снять меньше ММР(${value.to2Digits()} чем минимальный лимит по рангу: $minRemoved (rankValue:${rank.rankValue} * 0.5). Снимаем лимит.\n"
            value = minRemoved
        }

        mmrEmailText += "[calcRemoveMMR] итого снимаем ММР: ${value.to2Digits()}\n"

        return value.to2Digits()
    }

    /**
     * Подсчет увеличения бонусных ММР
     */
    private fun calcAddSavedMMR(kordlol: KORDLOLs) : Double {
        val value: Double

        //текущее значение бонусных ММР
        val newSavedMMR = kordlol.mmrAramSaved.to2Digits()

        val rank = EnumMMRRank.getMMRRank(kordlol.mmrAram)

        //лимит получаемых бонусных ММР за матч
        val limitMMR = rank.rankValue * 3.0

        mmrEmailText += "\n[calcAddSavedMMR] текущее значение бонус ММР: $newSavedMMR\n"

        //подсчет добавочных бонусных ММР
        var addSavedMMR = 0.0

        //за каждую пенту 5 очков
        if (participant.kills5 > 0) {
            mmrEmailText += "[calcAddSavedMMR] сделано Пент: ${participant.kills5}\n"
            addSavedMMR += participant.kills5 * 5.0
        }

        //за каждую квадру 3 очка
        if (participant.kills4 > 0) {
            mmrEmailText += "[calcAddSavedMMR] сделано Квадр: ${participant.kills4}\n"
            addSavedMMR += participant.kills4 * 3.0
        }

        //за каждую триплу 1 очко
        if (participant.kills3 > 0) {
            mmrEmailText += "[calcAddSavedMMR] сделано Трипл: ${participant.kills3}\n"
            addSavedMMR += participant.kills3
        }

        if (addSavedMMR > limitMMR) {
            mmrEmailText += "[calcAddSavedMMR] попытка получить больше ММР $addSavedMMR чем лимит $limitMMR\n"
            addSavedMMR = limitMMR
        }

        value = newSavedMMR + addSavedMMR

        mmrEmailText += "[calcAddSavedMMR] Добавляем бонусные ММР: ${addSavedMMR.to2Digits()}\n"

        return value.to2Digits()
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
            mmrEmailText += "[calcRemSavedMMR] бонусных ММР $newSavedMMR больше либо равно чем вычитаем: $minusMMR. Вычитаем 0. Осталось бонусных: ${(newSavedMMR - minusMMR).to2Digits()}\n"
            newSavedMMR -= minusMMR
            minusMMR = 0.0
        } else {
            mmrEmailText += "[calcRemSavedMMR] бонусных ММР $newSavedMMR меньше вычитаемых ММР: $minusMMR. Бонусных 0. Вычитаем всего ${(minusMMR - newSavedMMR).to2Digits()}\n"
            minusMMR -= newSavedMMR
            newSavedMMR = 0.0
        }

        mmrEmailText += "[calcRemSavedMMR] результат бонусных ММР: ${newSavedMMR.to2Digits()} вычитаемых ММР: ${minusMMR.to2Digits()}\n"

        return Pair(newSavedMMR, minusMMR)
    }

    private fun calculateField(propertyParticipant: KMutableProperty1<Participants, *>, propertyMmr: KMutableProperty1<MMRs, *>, minValue: Double? = null, maxMMR: Double? = null) {
        if (mmrTable == null) return

        val valuePropertyMmr = ((propertyMmr.invoke(mmrTable!!) as Double) * baseModificator).to2Digits()
        val valuePropertyParticipant = when (val valuePart = propertyParticipant.invoke(participant)){
            is Int -> valuePart.toDouble()
            is Double -> valuePart
            is Long -> valuePart.toDouble()
            else -> {
                printLog("[calculateField] Fail convert field Participant ${propertyParticipant.name} to Double: $valuePart")
                0.0
            }
        }.to2Digits()

        if (minValue != null && valuePropertyMmr < (minValue * mmrModificator).to2Digits()) {
            mmrEmailText += "[FIELDS] ${propertyParticipant.name} текущее значение $valuePropertyMmr меньше требуемого: ${(minValue * mmrModificator).to2Digits()}. пропускаем поле\n"
            return
        }
        countFields++

        var localMMR = valuePropertyParticipant.fromDoublePerc(valuePropertyMmr * mmrModificator).to2Digits()
        if (maxMMR != null && localMMR > maxMMR) {
            "[FIELDS] ${propertyMmr.name} получено ММР $localMMR за поле $valuePropertyParticipant больше лимита $maxMMR. Устанавливаем ММР в лимит\n"
            localMMR = maxMMR
        }

        mmrValue += localMMR
        mmrValue = mmrValue.to2Digits()

        mmrEmailText += "[FIELDS] ${propertyMmr.name} текущее значение: $valuePropertyParticipant. необходимо: ${(valuePropertyMmr * mmrModificator).to2Digits()}. Вышло ММР: $localMMR\n"
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
        return "CalculateMMR(win=${participant.win} mmrValue=$mmrValue, countFields=$countFields)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CalculateMMR_2

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