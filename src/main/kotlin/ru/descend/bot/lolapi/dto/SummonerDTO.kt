package ru.descend.bot.lolapi.dto

data class SummonerDTO(
    val accountId: String,
    val id: String,
    val name: String,
    val profileIconId: Int,
    val puuid: String,
    val revisionDate: Long,
    val summonerLevel: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SummonerDTO

        if (accountId != other.accountId) return false
        if (id != other.id) return false
        if (name != other.name) return false
        if (profileIconId != other.profileIconId) return false
        if (puuid != other.puuid) return false
        if (revisionDate != other.revisionDate) return false
        if (summonerLevel != other.summonerLevel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = accountId.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + profileIconId
        result = 31 * result + puuid.hashCode()
        result = 31 * result + revisionDate.hashCode()
        result = 31 * result + summonerLevel
        return result
    }
}