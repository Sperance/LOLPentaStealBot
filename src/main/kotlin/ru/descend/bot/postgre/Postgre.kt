package ru.descend.bot.postgre

import config

object PostgreSQL {

    private const val POSTGRES_URL = "jdbc:postgresql://localhost:5432/postgres"
    private const val POSTGRES_USERNAME = "postgres"
    private const val POSTGRES_PASSWORD = "22322137"

    fun initializePostgreSQL() {
        config {
            database = databases.PostgreSQL(
                url = POSTGRES_URL,
                user = POSTGRES_USERNAME,
                password = POSTGRES_PASSWORD
            )
            setTables(::firePersonTable, ::fireGuildTable)
        }
        println("POSTGRE_SQL $POSTGRES_URL initialized")
    }
}