package ru.descend.bot.savedObj

import ru.descend.bot.postgre.tables.TableParticipant
import ru.descend.bot.toModMax

class MMRCalculate(private val participant: TableParticipant) {

    private var currentMMR = 0.0
    private var currentMMRstr = ""
    private var maxPointOne = 4.0

    fun getMMR() = currentMMR
    fun getMMRstr() = currentMMRstr

    init {
        setKda(4.0)
        setTeamDamagePercentage(10.0)
        setDamageDealtToBuildings(2500.0)
    }

    fun setSkillsCast(mod: Double) {
        currentMMRstr += "skillsCast: ${participant.skillsCast} mod: $mod ; \t"
        currentMMR += participant.skillsCast.toModMax(mod, maxPointOne)
    }
    fun setTotalHealsOnTeammates(mod: Double) {
        currentMMRstr += "totalHealsOnTeammates: ${participant.totalHealsOnTeammates} mod: $mod ; \t"
        currentMMR += participant.totalHealsOnTeammates.toModMax(mod, maxPointOne)
    }
    fun setTotalDamageShieldedOnTeammates(mod: Double) {
        currentMMRstr += "totalDamageShieldedOnTeammates: ${participant.totalDamageShieldedOnTeammates} mod: $mod ; \t"
        currentMMR += participant.totalDamageShieldedOnTeammates.toModMax(mod, maxPointOne)
    }
    fun setDamageDealtToBuildings(mod: Double) {
        currentMMRstr += "damageDealtToBuildings: ${participant.damageDealtToBuildings} mod: $mod ; \t"
        currentMMR += participant.damageDealtToBuildings.toModMax(mod, maxPointOne)
    }
    fun setTimeCCingOthers(mod: Double) {
        currentMMRstr += "timeCCingOthers: ${participant.timeCCingOthers} mod: $mod ; \t"
        currentMMR += participant.timeCCingOthers.toModMax(mod, maxPointOne)
    }
    fun setEnemyChampionImmobilizations(mod: Double) {
        currentMMRstr += "enemyChampionImmobilizations: ${participant.enemyChampionImmobilizations} mod: $mod ; \t"
        currentMMR += participant.enemyChampionImmobilizations.toModMax(mod, maxPointOne)
    }
    fun setDamageTakenOnTeamPercentage(mod: Double) {
        currentMMRstr += "damageTakenOnTeamPercentage: ${participant.damageTakenOnTeamPercentage} mod: $mod ; \t"
        currentMMR += participant.damageTakenOnTeamPercentage * mod
    }
    private fun setKda(mod: Double) {
        currentMMRstr += "kda: ${participant.kda} mod: $mod ; \t"
        currentMMR += participant.kda.toModMax(mod, maxPointOne)
    }
    private fun setTeamDamagePercentage(mod: Double) {
        currentMMRstr += "teamDamagePercentage: ${participant.teamDamagePercentage} mod: $mod ; \t"
        currentMMR += participant.teamDamagePercentage * mod
    }
    fun setMinionsKills(mod: Double) {
        currentMMRstr += "kda: ${participant.minionsKills} mod: $mod ; \t"
        currentMMR += participant.minionsKills.toModMax(mod, maxPointOne)
    }
    private fun setInhibitorKills(mod: Double) {
        currentMMRstr += "kda: ${participant.inhibitorKills} mod: $mod ; \t"
        currentMMR += participant.inhibitorKills.toModMax(mod, maxPointOne)
    }
}