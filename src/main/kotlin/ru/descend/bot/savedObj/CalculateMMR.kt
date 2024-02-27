package ru.descend.bot.savedObj

import ru.descend.bot.asyncLaunch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lowDescriptor
import ru.descend.bot.postgre.SQLData
import ru.descend.bot.postgre.tables.TableKORD_LOL
import ru.descend.bot.postgre.tables.TableMatch
import ru.descend.bot.postgre.tables.TableMmr
import ru.descend.bot.postgre.tables.TableParticipant
import ru.descend.bot.printLog
import ru.descend.bot.sendMessage
import ru.descend.bot.to2Digits
import ru.descend.bot.toDate
import ru.descend.bot.toFormatDateTime
import update
import kotlin.math.abs
import kotlin.reflect.KMutableProperty1

private const val modRank = 70.0
private const val modTitle = 100.0

enum class EnumMMRRank(val nameRank: String, val minMMR: Double, val rankValue: Int) {
    UNRANKED("Нет ранга", 0.0, 0),
    PAPER_III("Бумага III", UNRANKED.minMMR + modRank, 0),
    PAPER_II("Бумага II", PAPER_III.minMMR + modRank, 0),
    PAPER_I("Бумага I", PAPER_II.minMMR + modRank, 0),
    WOOD_III("Дерево III", PAPER_I.minMMR + modTitle, 1),
    WOOD_II("Дерево II", WOOD_III.minMMR + modRank, 1),
    WOOD_I("Дерево I", WOOD_II.minMMR + modRank, 1),
    IRON_III("Железо III", WOOD_I.minMMR + modTitle, 2),
    IRON_II("Железо II", IRON_III.minMMR + modRank, 2),
    IRON_I("Железо I", IRON_II.minMMR + modRank, 2),
    BRONZE_III("Бронза III", IRON_I.minMMR + modTitle, 3),
    BRONZE_II("Бронза II", BRONZE_III.minMMR + modRank, 3),
    BRONZE_I("Бронза I", BRONZE_II.minMMR + modRank, 3),
    SILVER_III("Серебро III", BRONZE_I.minMMR + modTitle, 4),
    SILVER_II("Серебро II", SILVER_III.minMMR + modRank, 4),
    SILVER_I("Серебро I", SILVER_II.minMMR + modRank, 4),
    GOLD_III("Золото III", SILVER_I.minMMR + modTitle, 5),
    GOLD_II("Золото II", GOLD_III.minMMR + modRank, 5),
    GOLD_I("Золото I", GOLD_II.minMMR + modRank, 5),
    PLATINUM_III("Платина III", GOLD_I.minMMR + modTitle, 6),
    PLATINUM_II("Платина II", PLATINUM_III.minMMR + modRank, 6),
    PLATINUM_I("Платина I", PLATINUM_II.minMMR + modRank, 6),
    DIAMOND_III("Алмаз III", PLATINUM_I.minMMR + modTitle, 7),
    DIAMOND_II("Алмаз II", DIAMOND_III.minMMR + modRank, 7),
    DIAMOND_I("Алмаз I", DIAMOND_II.minMMR + modRank, 7),
    MASTER_III("Мастер III", DIAMOND_I.minMMR + modTitle, 8),
    MASTER_II("Мастер II", MASTER_III.minMMR + modRank, 8),
    MASTER_I("Мастер I", MASTER_II.minMMR + modRank, 8),
    GRANDMASTER_III("ГрандМастер III", MASTER_I.minMMR + modTitle, 9),
    GRANDMASTER_II("ГрандМастер II", GRANDMASTER_III.minMMR + modRank, 9),
    GRANDMASTER_I("ГрандМастер I", GRANDMASTER_II.minMMR + modRank, 9),
    CHALLENGER("Челленджер", GRANDMASTER_I.minMMR + modTitle, 10)
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

class CalculateMMR(private val sqlData: SQLData, private val participant: TableParticipant, val match: TableMatch, kordlol: List<TableKORD_LOL>, private val mmrTable: TableMmr?) {

    private var mmrValue = 0.0
    private var mmrValueStock = 0.0
    private var mmrText = ""
    private var mmrExtendedText = ""
    private var mmrModificator = 1.0
    private var countFields = 0.0

    private var baseModificator = 1.25 //25%

    init {
        if (mmrTable != null) {
            mmrModificator = (match.matchDuration.toDouble().fromDoubleValue(mmrTable.matchDuration) / 100.0).to2Digits()
            if (mmrModificator < 0) {
                printLog("[CalculateMMR] mmrModificator < 0: $mmrModificator. Match: ${match.matchId}. Setting modificator 1.0")
                mmrExtendedText += ";mmrModificator < 0 ($mmrModificator), setting 1.0"
                mmrModificator = 1.0
            }

            var isFighterTank = false
            var isMageSupport = false
            LeagueMainObject.catchHeroForId(participant.championId.toString())?.let {
                if (it.tags.contains("Fighter") || it.tags.contains("Tank")) isFighterTank = true
                if (it.tags.contains("Mage") || it.tags.contains("Support")) isMageSupport = true
            }

            mmrText += ";isWarriorTank=$isFighterTank"
            mmrText += ";isMageSupport=$isMageSupport"

            countFields = 0.0
            calculateField(TableParticipant::minionsKills, TableMmr::minions)
            calculateField(TableParticipant::skillsCast, TableMmr::skills)

            if (isMageSupport) {
                calculateField(TableParticipant::totalDamageShieldedOnTeammates, TableMmr::shielded, 100.0)
                calculateField(TableParticipant::totalHealsOnTeammates, TableMmr::healed, 100.0)
            }

//          calculateField(TableParticipant::damageDealtToBuildings, TableMmr::dmgBuilding)
            calculateField(TableParticipant::timeCCingOthers, TableMmr::controlEnemy, 3.0)

            if (!isMageSupport && isFighterTank) {
                calculateField(TableParticipant::enemyChampionImmobilizations, TableMmr::immobiliz, 5.0)
                calculateField(TableParticipant::damageTakenOnTeamPercentage, TableMmr::dmgTakenPerc)
            }
//            calculateField(TableParticipant::skillshotsDodged, TableMmr::skillDodge)

            calculateField(TableParticipant::teamDamagePercentage, TableMmr::dmgDealPerc)
            calculateField(TableParticipant::kda, TableMmr::kda)

            if (countFields < 7) countFields++

            kordlol.find { it.LOLperson?.LOL_puuid == participant.LOLperson?.LOL_puuid }?.let {
                calculateMMRaram(it)
            }

            if (match.matchMode != "ARAM") mmrText = ""
        }
    }

    private fun calculateMMRaram(kordlol: TableKORD_LOL?) {
        if (kordlol == null) return
        if (match.matchMode != "ARAM") return
        if (match.surrender) return
        if (match.bots) return

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
        kordlol.update(TableKORD_LOL::mmrAram, TableKORD_LOL::mmrAramSaved){
            mmrAram = newAramValue.to2Digits()
            mmrAramSaved = newSavedMMR.to2Digits()
        }
        participant.update(TableParticipant::mmr){
            mmr = partMMR.to2Digits()
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

        //Ранг повышен
        if (oldRank.minMMR < newRank.minMMR) {
            value += 10.0
            mmrExtendedText += ";new Rank: ${newRank.nameRank}"
        }

        //Ранг понижен
        if (oldRank.minMMR > newRank.minMMR) {
//            value += 10.0
            mmrExtendedText += ";removed new Rank: ${oldRank.nameRank}"
        }

        return value.to2Digits()
    }

    /**
     * Подсчет ММР которое даётся игроку (при победе)
     */
    private fun calcAddingMMR(kordlol: TableKORD_LOL) : Double {
        //Текущее значение ММР + новое значение ММР
        var value = kordlol.mmrAram + mmrValue

        //обработка добавочного ММР за лузстрик
        val looseStreak = sqlData.getWinStreak()[kordlol.LOLperson?.id]?:0
        if (looseStreak < -2) {
            value += abs(looseStreak) * 0.3
            mmrExtendedText += ";add MMR for looseStreak($looseStreak): ${abs(looseStreak) * 0.3}"
        }

        //обработка штрафа к получаемому ММР в зависимости от ранга
        val rank = EnumMMRRank.getMMRRank(kordlol.mmrAram)
        val removeMMR = (rank.rankValue / 10.0)
        mmrExtendedText += ";removed MMR for Rank ${rank.nameRank} removed: $removeMMR"
        value -= removeMMR
        if (value <= 0.0) {
            value = (value + removeMMR) / 2.0
            mmrExtendedText += ";below zero MMR. Setting in ${(value + removeMMR) / 2.0}"
        }

        return value.to2Digits()
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

        //обработка минимального кол-ва снятия в зависимости от ранга
        val rank = EnumMMRRank.getMMRRank(kordlol.mmrAram)
        val minimumMinus = ((rank.rankValue / 10.0) * 5.0) + 1.0
        if (value < minimumMinus) {
            mmrExtendedText += ";remove MMR first: $value low that minimum: $minimumMinus. Setted minimum"
            value = minimumMinus
        }

        //если снимаем больше чем полей*2 - это много, органичиваем общим числом полей
        if (value > countFields) {
            mmrExtendedText += ";very strong removed($value). Setted to $countFields"
            value = countFields
        }

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

        //подсчет добавочных бонусных ММР
        var addSavedMMR = 0.0
        addSavedMMR += participant.kills5 * 5.0             //за каждую пенту 5 очков
        addSavedMMR += participant.kills4 * 2.0             //за каждую квадру 2 очка
        if (addSavedMMR > limitMMR) {
            mmrExtendedText += ";adding so many mmr: $addSavedMMR Setting to limit $limitMMR"
            addSavedMMR = limitMMR
        }

        value = newSavedMMR + addSavedMMR

        return value.to2Digits()
    }

    /**
     * Вычитаем ММР из бонусных
     */
    private fun calcRemSavedMMR(_newSavedMMR: Double, _minusMMR: Double) : Pair<Double, Double> {

        var newSavedMMR = _newSavedMMR
        var minusMMR = _minusMMR

        if (newSavedMMR > 0.0) {
            newSavedMMR -= minusMMR                     //вычитаем ММР из общего числа бонусных ММР
        }
        if (newSavedMMR < 0.0) {                        //если вычитаем Больше чем есть в бонусных ММР - вычитаем остатки из minusMMR
            minusMMR -= abs(newSavedMMR)
            if (minusMMR < 0.0) minusMMR = 0.0          //сохранить или вычесть меньше 0 ММР нельзя
            newSavedMMR = 0.0
        }

        return Pair(newSavedMMR, minusMMR)
    }

    private fun calculateField(propertyParticipant: KMutableProperty1<TableParticipant, *>, propertyMmr: KMutableProperty1<TableMmr, *>, limitValue: Double? = null) {
        if (mmrTable == null) return

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

        if (limitValue != null && valuePropertyMmr < (limitValue * mmrModificator).to2Digits()) {
            mmrExtendedText += ";field ${propertyParticipant.name} with value $valuePropertyMmr low than minimum: ${(limitValue * mmrModificator).to2Digits()}"
            return
        }
        countFields++

        val localMMR = valuePropertyParticipant.fromDoublePerc(valuePropertyMmr * mmrModificator).to2Digits()
        mmrValue += localMMR
        mmrValue = mmrValue.to2Digits()

        val localMMRStock = valuePropertyParticipant.fromDoublePerc(valuePropertyMmrStock * mmrModificator).to2Digits()
        mmrValueStock += localMMRStock
        mmrValueStock = mmrValueStock.to2Digits()

        mmrText += ";${propertyMmr.name}:$valuePropertyParticipant(${(valuePropertyMmr * mmrModificator).to2Digits()})=$localMMR"
    }

    private fun Double.fromDoubleValue(stock: Double): Double {
        return ((this / stock) * 100.0)
    }

    private fun Double.fromDoublePerc(stock: Double): Double {
        // 40 крипов - по стате 200 норма - это 20% от нормы
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
        return "CalculateMMR(win=${participant.win} mmrValue=$mmrValue, mmrExtendedText='$mmrExtendedText', mmrText='$mmrText', countFields=$countFields)"
    }
}