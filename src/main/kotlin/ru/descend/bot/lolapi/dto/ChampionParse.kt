package ru.descend.bot.lolapi.dto

import ru.descend.bot.lolapi.LeagueMainObject.LOL_VERSION
import java.util.HashMap

data class ChampionsDTO(
    val type: String,
    val format: String,
    val version: String,
    val data: HashMap<Any, Any>,
)

open class InterfaceChampionBase (
    val id: String = "",
    val key: String = "",
    val name: String = "",
    val tags: List<String> = listOf()
) {
    fun getIconURL() : String {
        return "https://ddragon.leagueoflegends.com/cdn/$LOL_VERSION/img/champion/$id.png"
    }

    override fun toString(): String {
        return "InterfaceChampionBase(id='$id', key='$key', name='$name', tags=$tags)"
    }
}