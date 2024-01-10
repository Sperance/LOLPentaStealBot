package ru.descend.bot.postgre

import config
import databases.PostgreSQL

object PostgreSQL {

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
            setTables(::fireGuildTable, ::fireMatchTable, ::fireParticipantTable, ::fireKORDPersonTable, ::fireLOLPersonTable, ::fireKORD_LOLPersonTable)
        }
        println("POSTGRE_SQL $POSTGRES_URL initialized")
    }
}