package ru.descend.bot.savedObj

import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.SQLData
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
    GRANDMASTER_III("ГрандМастер III", MASTER_I.minMMR + modTitle, 10),
    GRANDMASTER_II("ГрандМастер II", GRANDMASTER_III.minMMR + modRank, 10),
    GRANDMASTER_I("ГрандМастер I", GRANDMASTER_II.minMMR + modRank, 10),
    CHALLENGER("Челленджер", GRANDMASTER_I.minMMR + modTitle, 11)
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

class CalculateMMR(private var sqlData: SQLData, private var participant: TableParticipant, var match: TableMatch, kordlol: List<TableKORD_LOL>, private var mmrTable: TableMmr?) {

    private var mmrValue = 0.0
    private var mmrEmailText = ""
    private var mmrModificator = 1.0
    private var countFields = 0.0

    private var baseModificator = 1.3

    init {
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
            calculateField(TableParticipant::minionsKills, TableMmr::minions)
            calculateField(TableParticipant::skillsCast, TableMmr::skills)

            if (isMageSupport) {
                calculateField(TableParticipant::totalDamageShieldedOnTeammates, TableMmr::shielded, minValue = 300.0)
                calculateField(TableParticipant::totalHealsOnTeammates, TableMmr::healed, minValue = 300.0)
            }

            calculateField(TableParticipant::damageDealtToBuildings, TableMmr::dmgBuilding, minValue = 500.0, maxMMR = 1.0)
            calculateField(TableParticipant::timeCCingOthers, TableMmr::controlEnemy, minValue = 3.0)

            if (!isMageSupport && isFighterTank) {
                calculateField(TableParticipant::enemyChampionImmobilizations, TableMmr::immobiliz, minValue = 5.0)
                calculateField(TableParticipant::damageTakenOnTeamPercentage, TableMmr::dmgTakenPerc)
            }
            calculateField(TableParticipant::skillshotsDodged, TableMmr::skillDodge, minValue = 20.0, maxMMR = 1.0)

            calculateField(TableParticipant::teamDamagePercentage, TableMmr::dmgDealPerc)
            calculateField(TableParticipant::kda, TableMmr::kda)

            kordlol.find { it.LOLperson?.LOL_puuid == participant.LOLperson?.LOL_puuid }?.let {
                calculateMMRaram(it)
            }

            if (match.matchMode == "ARAM") {
                sqlData.guildSQL.sendEmail("${match.matchId} (${match.id})", mmrEmailText)
            }
            mmrEmailText = ""
        } else {
            printLog("MMR table is NULL")
        }
    }

    private fun calculateMMRaram(kordlol: TableKORD_LOL?) {
        if (kordlol == null) return
        if (match.matchMode != "ARAM") return
        if (match.surrender) return
        if (match.bots) return

        mmrEmailText += "\n[BEGIN] User(kordlol_id): ${kordlol.id} Summoner: ${kordlol.LOLperson?.LOL_summonerName} Champion: ${participant.championName} Win: ${participant.win}\n"
        mmrEmailText += "[BEGIN] MMR: ${kordlol.mmrAram.to2Digits()} SavedMMR: ${kordlol.mmrAramSaved.to2Digits()}\n\n"

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
        mmrEmailText += "[COMPLETED] calculateMMR: ${mmrValue.to2Digits()} mmrAram: ${newAramValue.to2Digits()} mmrAramSaved: ${newSavedMMR.to2Digits()} participant MMR: ${partMMR.to2Digits()}\n"
        kordlol.update(TableKORD_LOL::mmrAram, TableKORD_LOL::mmrAramSaved){
            mmrAram = newAramValue.to2Digits()
            mmrAramSaved = newSavedMMR.to2Digits()
        }
        participant.update(TableParticipant::mmr, TableParticipant::mmrFlat){
            mmr = partMMR.to2Digits()
            mmrFlat = mmrValue.to2Digits()
        }
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
    private fun calcAddingMMR(kordlol: TableKORD_LOL) : Double {
        //Текущее значение ММР + новое значение ММР
        var addedValue = mmrValue.to2Digits()
//        var value = kordlol.mmrAram + mmrValue

        mmrEmailText += "\n[calcAddingMMR] текущее значение ММР: ${kordlol.mmrAram} Из них посчитано добавочных: $addedValue\n"

        //обработка добавочного ММР за лузстрик
        val looseStreak = sqlData.getWinStreak()[kordlol.LOLperson?.id]?:0
        if (looseStreak < -2) {
            addedValue += (abs(looseStreak) * 0.3).to2Digits()
            mmrEmailText += "[calcAddingMMR] лузстрик: $looseStreak. Добавляем ММР к получаемым: ${(abs(looseStreak) * 0.3).to2Digits()} получилось ММР: $addedValue\n"
        }

        //обработка штрафа к получаемому ММР в зависимости от ранга
        val rank = EnumMMRRank.getMMRRank(kordlol.mmrAram)
        val removeMMR = (rank.rankValue / 10.0 * 2).to2Digits()
        if (removeMMR > 0.0) {
            mmrEmailText += "[calcAddingMMR] штраф за ранг: ${removeMMR.to2Digits()} было ММР: $addedValue стало: ${(addedValue - removeMMR).to2Digits()}\n"
            addedValue -= removeMMR
        }

        //обработка минимума получаемых ММР
        if (addedValue < (countFields / 3.0).to2Digits()) {
            mmrEmailText += "[calcAddingMMR] получаемых ММР $addedValue меньше лимита по полям ${(countFields / 3.0).to2Digits()}. Устанавливаем в лимит\n"
            addedValue = (countFields / 3.0).to2Digits()
        }

        mmrEmailText += "[calcAddingMMR] результат получаемых ММР: ${addedValue.to2Digits()}\n"

        return (kordlol.mmrAram + addedValue).to2Digits()
    }

    /**
     * Подсчет ММР которое вычитается из игрока (при поражении)
     */
    private fun calcRemoveMMR(kordlol: TableKORD_LOL) : Double {
        var value: Double

        value = if (mmrValue < countFields) {
            //кол-во всех полей минус текущий ММР (сколько нехватило до Нормы)
            (countFields - mmrValue) * 2.0
        } else {
            //иначе если катали лучше Нормы - снимаем жалкую единичку
            1.0
        }

        mmrEmailText += "\n[calcRemoveMMR] снимаем ММР: ${value.to2Digits()}\n"

        //если снимаем больше чем полей*2 - это много, органичиваем общим числом полей
        if (value > countFields) {
            mmrEmailText += "[calcRemoveMMR] попытка снять ${value.to2Digits()} больше чем кол-во полей($countFields) Снимаем ММР $countFields\n"
            value = countFields
        }

        //лимит на максимальное снятие
        val rank = EnumMMRRank.getMMRRank(kordlol.mmrAram)
        if (value > (rank.rankValue + 1.0)){
            mmrEmailText += "[calcRemoveMMR] попытка снять больше ММР(${value.to2Digits()} чем лимит по рангу: ${(rank.rankValue + 1.0).to2Digits()}). Снимаем лимит.\n"
            value = rank.rankValue + 1.0
        }

        //лимит на минимальное снятие
        if (value < (rank.rankValue * 0.5).to2Digits()) {
            mmrEmailText += "[calcRemoveMMR] попытка снять меньше ММР(${value.to2Digits()} чем лимит по рангу: ${(rank.rankValue * 0.5).to2Digits()}). Снимаем лимит.\n"
            value = rank.rankValue * 0.5
        }

        mmrEmailText += "[calcRemoveMMR] итого снимаем ММР: ${value.to2Digits()}\n"

        return value.to2Digits()
    }

    /**
     * Подсчет увеличения бонусных ММР
     */
    private fun calcAddSavedMMR(kordlol: TableKORD_LOL) : Double {
        val value: Double

        //текущее значение бонусных ММР
        val newSavedMMR = kordlol.mmrAramSaved.to2Digits()

        //лимит получаемых бонусных ММР за матч
        val limitMMR = 10.0

        mmrEmailText += "\n[calcAddSavedMMR] текущее значение бонус ММР: $newSavedMMR. Лимит получаемых бонусных ММР: $limitMMR\n"

        //подсчет добавочных бонусных ММР
        var addSavedMMR = 0.0

        //за каждую пенту 5 очков
        if (participant.kills5 > 0) {
            mmrEmailText += "[calcAddSavedMMR] сделано Пент: ${participant.kills5} добавляем ${participant.kills5 * 5.0} ММР\n"
            addSavedMMR += participant.kills5 * 5.0
        }

        //за каждую квадру 2 очка
        if (participant.kills4 > 0) {
            mmrEmailText += "[calcAddSavedMMR] сделано Квадр: ${participant.kills4} добавляем ${participant.kills4 * 2.0} ММР\n"
            addSavedMMR += participant.kills4 * 2.0
        }

        if (addSavedMMR > limitMMR) {
            mmrEmailText += "[calcAddSavedMMR] попытка получить больше ММР $addSavedMMR чем лимит $limitMMR\n"
            addSavedMMR = limitMMR
        }

        value = newSavedMMR + addSavedMMR

        mmrEmailText += "[calcAddSavedMMR] Добавили бонусных ММР: ${addSavedMMR.to2Digits()} новое значение бонусных ММР: ${value.to2Digits()}\n"

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

    private fun calculateField(propertyParticipant: KMutableProperty1<TableParticipant, *>, propertyMmr: KMutableProperty1<TableMmr, *>, minValue: Double? = null, maxMMR: Double? = null) {
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

        other as CalculateMMR

        if (sqlData != other.sqlData) return false
        if (participant != other.participant) return false
        if (match != other.match) return false
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
        result = 31 * result + (mmrTable?.hashCode() ?: 0)
        result = 31 * result + mmrValue.hashCode()
        result = 31 * result + mmrEmailText.hashCode()
        result = 31 * result + mmrModificator.hashCode()
        result = 31 * result + countFields.hashCode()
        result = 31 * result + baseModificator.hashCode()
        return result
    }
}