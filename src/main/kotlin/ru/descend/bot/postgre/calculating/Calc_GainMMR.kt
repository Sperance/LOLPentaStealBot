package ru.descend.bot.postgre.calculating

import ru.descend.bot.BONUS_MMR_FOR_LVP_ARAM
import ru.descend.bot.BONUS_MMR_FOR_MVP_ARAM
import ru.descend.bot.BONUS_MMR_FOR_NEW_RANK
import ru.descend.bot.BONUS_MMR_FOR_WIN
import ru.descend.bot.LIMIT_BINUS_MMR_FOR_MATCH
import ru.descend.bot.LVP_TAG
import ru.descend.bot.MVP_TAG
import ru.descend.bot.generateAIText
import ru.descend.bot.launch
import ru.descend.bot.lowDescriptor
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.printLog
import ru.descend.bot.sendMessage
import ru.descend.bot.to1Digits
import ru.descend.bot.toFormatDateTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Calc_GainMMR(private var participant: ParticipantsNew, private var lols: LOLs, private var sqldata: SQLData_R2DBC, private var match: Matches) {

    private var textTemp = lols.toString() + "\n"

    fun getTempText() = textTemp

    init {
        //Обработка бонусов и победы\поражения
        calcSavedMMR()
        if (participant.win) {
            calcWinMMR(participant.gameMatchMmr)
        } else {
            calcLooseMMR(participant.gameMatchMmr)
        }
        textTemp += "[Calc_GainMMR::РЕЗУЛЬТАТ]: $lols\n"
    }

    private fun calcWinMMR(stockMMR: Double) {
        var addedValue = stockMMR.to1Digits()

        //Штраф за победу на высоком ранге
        val removeMMR = (lols.getRank().rankValue / 10.0).to1Digits()
        if (removeMMR > 0.0) {
            textTemp += "[calcWinMMR::ШтрафПобеды] было: $addedValue вычитаем $removeMMR\n"
            addedValue -= removeMMR
        }

        //обработка минимума получаемых ММР
        val minimumGainMMRwin = lols.calcMinGainedMMR()
        if (addedValue < minimumGainMMRwin) {
            textTemp += "[calcWinMMR::МинимумПобеды] было: $addedValue стало $minimumGainMMRwin\n"
            addedValue = minimumGainMMRwin
        }

        if (participant.gameMatchKey == MVP_TAG) {
            addedValue += BONUS_MMR_FOR_MVP_ARAM
            textTemp += "[calcLooseMMR::MVP] добавляем еще $BONUS_MMR_FOR_MVP_ARAM стало $addedValue\n"
        }

        val oldRank = lols.getRank()

        textTemp += "[calcWinMMR::ИтогПобеды] было: ${lols.mmrAram} добавляем ${addedValue.to1Digits()}\n"

        lols.mmrAram = (lols.mmrAram + addedValue).to1Digits()
        participant.gameMatchMmr = addedValue.to1Digits()

        //Если повысился ранг - даём сверху 10 ММР бонуса
        if (oldRank.ordinal < lols.getRank().ordinal) {
            lols.mmrAramSaved = (lols.mmrAramSaved + BONUS_MMR_FOR_NEW_RANK + lols.getRank().ordinal).to1Digits()
        }
    }

    private fun calcLooseMMR(stockMMR: Double) {
        var value = abs(stockMMR)
        textTemp += "[calcLooseMMR::Поражение] стоковое снятие: $value\n"

        //лимит на минимальное снятие
        val minRemoved = lols.calcMinRemovedMMR()
        textTemp += "[calcLooseMMR::ПоражениеМинимум] попытка снять $value минимум снимания $minRemoved\n"
        if (value < minRemoved) {
            textTemp += "[calcLooseMMR::ПоражениеМинимум] было: $value стало $minRemoved\n"
            value = minRemoved
        }

        //лимит на максимальное снятие
        val maxRemoved = lols.calcMaxRemovedMMR()
        if (value > maxRemoved){
            textTemp += "[calcLooseMMR::ПоражениеМаксимум] было: $value стало $maxRemoved\n"
            value = maxRemoved
        }

        if (participant.gameMatchKey == LVP_TAG) {
            value += BONUS_MMR_FOR_LVP_ARAM
            textTemp += "[calcLooseMMR::LVP] вычитаем еще $BONUS_MMR_FOR_LVP_ARAM стало $value\n"
        }

        textTemp += "[calcLooseMMR::ИтогПоражение] было: ${lols.mmrAram} вычитаем: $value\n"

        lols.removeMMRvalue(value.to1Digits())
        participant.gameMatchMmr = -value.to1Digits()
    }

    private fun calcSavedMMR() {
        val value: Double

        //текущее значение бонусных ММР
        val newSavedMMR = lols.mmrAramSaved.to1Digits()

        //подсчет добавочных бонусных ММР
        var addSavedMMR = 0.0

        //за каждую пенту 5 очков
        if (participant.kills5 > 0) {
            addSavedMMR += participant.kills5 * 5.0
            textTemp += "[calcSavedMMR::Пентакиллы] количество: ${participant.kills5} добавляем бонуса ${participant.kills5 * 5.0}\n"
            sqldata.calculatePentakill(lols, participant, match)
        }

        //за каждую квадру 3 очка
        if (participant.kills4 > 0) {
            addSavedMMR += participant.kills4 * 3.0
            textTemp += "[calcSavedMMR::Квадры] количество: ${participant.kills4} добавляем бонуса ${participant.kills4 * 3.0}\n"
        }

        val limitBonus = LIMIT_BINUS_MMR_FOR_MATCH + lols.getRank().rankValue
        if (addSavedMMR > limitBonus) {
            textTemp += "[calcSavedMMR::ЛимитБонуса] лимит: $limitBonus до лимита: $addSavedMMR\n"
            addSavedMMR = limitBonus
        }

        if (participant.win) {
            addSavedMMR += BONUS_MMR_FOR_WIN
            textTemp += "[calcSavedMMR::Просто победа] добавляем $BONUS_MMR_FOR_WIN. Стало: $addSavedMMR\n"
        }

        value = newSavedMMR + addSavedMMR

        textTemp += "[calcSavedMMR::ИтогБонусных] было: $newSavedMMR добавляем: $addSavedMMR\n"

        lols.mmrAramSaved = value.to1Digits()
    }
}