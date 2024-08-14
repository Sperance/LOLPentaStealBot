package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.core.dsl.Meta

@KomapperEntity
@KomapperTable("tbl_heroes")
data class Heroes(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var nameEN: String = "",
    var nameRU: String = "",
    var otherNames: String = "",
    var key: String = ""
) {

    companion object {
        val tbl_heroes = Meta.heroes
    }

    override fun toString(): String {
        return "Heroes(id=$id, name='$nameRU', key='$key', other='$otherNames')"
    }
}