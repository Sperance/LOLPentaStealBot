package ru.descend.bot.postgre

import config
import databases.PostgreSQL
import ru.descend.bot.postgre.tables.tableGuild
import ru.descend.bot.postgre.tables.tableKORDLOL
import ru.descend.bot.postgre.tables.tableKORDPerson
import ru.descend.bot.postgre.tables.tableLOLPerson
import ru.descend.bot.postgre.tables.tableMatch
import ru.descend.bot.postgre.tables.tableParticipant

object Postgre {

    private const val POSTGRES_URL = "jdbc:postgresql://localhost:5432/postgres"
    private const val POSTGRES_USERNAME = "postgres"
    private const val POSTGRES_PASSWORD = "22322137"

    fun initializePostgreSQL() {
        config {
            database = PostgreSQL(
                url = POSTGRES_URL,
                user = POSTGRES_USERNAME,
                password = POSTGRES_PASSWORD
            )
            setTables(::tableGuild, ::tableMatch, ::tableParticipant, ::tableKORDPerson, ::tableLOLPerson, ::tableKORDLOL)
        }
        println("POSTGRE_SQL $POSTGRES_URL initialized")
    }
}