package ru.descend.bot.postgre

import config
import databases.PostgreSQL
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.r2dbc.R2dbcDatabase
import org.komapper.tx.core.TransactionAttribute
import org.komapper.tx.core.TransactionProperty
import ru.descend.bot.postgre.tables.tableGuild
import ru.descend.bot.postgre.tables.tableKORDLOL
import ru.descend.bot.postgre.tables.tableKORDPerson
import ru.descend.bot.postgre.tables.tableLOLPerson
import ru.descend.bot.postgre.tables.tableMatch
import ru.descend.bot.postgre.tables.tableMmr
import ru.descend.bot.postgre.tables.tableParticipant
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

object Postgre {

    private const val POSTGRES_URL = "jdbc:postgresql://localhost:5432/postgres"
    private const val POSTGRES_USERNAME = "postgres"
    private const val POSTGRES_PASSWORD = "22322137"

    private val connection: Connection = run {
        var count = 0
        while (count++ < Config.connectionAttemptsAmount) {
            runCatching { DriverManager.getConnection(POSTGRES_URL, POSTGRES_USERNAME, POSTGRES_PASSWORD) }.onSuccess { return@run it }
            Thread.sleep(Config.connectionAttemptsDelay)
        }
        throw Exception("Can't get connection  after $count attempts")
    }

    private val openedStatements = mutableListOf<Statement>()
    val newStatement: Statement
        get() = connection.createStatement().also { openedStatements.add(it) }

    fun initializePostgreSQL() {
        config {
            database = PostgreSQL(
                url = POSTGRES_URL,
                user = POSTGRES_USERNAME,
                password = POSTGRES_PASSWORD
            )
            setTables(::tableGuild, ::tableMatch, ::tableParticipant, ::tableKORDPerson, ::tableLOLPerson, ::tableKORDLOL, ::tableMmr)
        }
        println("POSTGRE_SQL $POSTGRES_URL initialized")
    }

    fun closeAllStatements() {
        openedStatements.forEach { it.close() }
        openedStatements.clear()
    }
}