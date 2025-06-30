package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import io.r2dbc.spi.ConnectionFactoryOptions
import org.komapper.core.ExecutionOptions
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.expression.ScalarExpression
import org.komapper.core.dsl.expression.SortExpression
import org.komapper.core.dsl.expression.WhereDeclaration
import org.komapper.core.dsl.operator.desc
import org.komapper.core.dsl.query.Query
import org.komapper.core.dsl.query.QueryScope
import org.komapper.core.dsl.query.firstOrNull
import org.komapper.r2dbc.R2dbcDatabase
import org.komapper.tx.core.CoroutineTransactionOperator
import org.komapper.tx.core.TransactionAttribute
import org.komapper.tx.core.TransactionProperty
import ru.descend.bot.datas.WorkData
import ru.descend.bot.datas.getData
import ru.descend.bot.datas.getDataOne
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.Guilds.Companion.tbl_guilds
import ru.descend.bot.postgre.r2dbc.model.Heroes
import ru.descend.bot.postgre.r2dbc.model.Heroes.Companion.tbl_heroes
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs.Companion.tbl_kordlols
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.KORDs.Companion.tbl_kords
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.MMRs.Companion.tbl_mmrs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Matches.Companion.tbl_matches
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew.Companion.tbl_participantsnew
import ru.descend.bot.printLog
import java.time.Duration
import kotlin.time.Duration.Companion.minutes

private val connectionFactory: ConnectionFactoryOptions = ConnectionFactoryOptions.builder()
    .option(ConnectionFactoryOptions.DRIVER, "postgresql")
    .option(ConnectionFactoryOptions.HOST, "localhost")
    .option(ConnectionFactoryOptions.PORT, 5432)
    .option(ConnectionFactoryOptions.USER, "postgres")
    .option(ConnectionFactoryOptions.PASSWORD, "22322137")
    .option(ConnectionFactoryOptions.DATABASE, "postgres2")
    .build()

val db = R2dbcDatabase(connectionFactory, executionOptions = ExecutionOptions(queryTimeoutSeconds = 300, suppressLogging = true))

/**
 * https://www.komapper.org/docs/
 */
object R2DBC {
    val stockHEROES = WorkData<Heroes>("HEROES")
    val stockMMR = WorkData<MMRs>("MMR")

    suspend fun runTransaction(body: suspend CoroutineTransactionOperator.() -> Unit) = db.withTransaction { body.invoke(it) }
    suspend fun runStrongTransaction(body: suspend CoroutineTransactionOperator.() -> Unit) = db.withTransaction(transactionAttribute = TransactionAttribute.REQUIRES_NEW) { body.invoke(it) }

    suspend fun <T> runQuery(query: Query<T>) = db.withTransaction { db.runQuery { query } }
    suspend fun <T> runQuery(block: QueryScope.() -> Query<T>) =
        db.withTransaction(
            transactionAttribute = TransactionAttribute.REQUIRED,
            transactionProperty = TransactionProperty.LockWaitTime(Duration.ofMinutes(2))
        ) { tx ->
            val query = block(QueryScope)
            runQuery(query)
        }

    suspend fun initialize() {
        db.withTransaction {
            db.runQuery { QueryDsl.create(tbl_guilds) }
            db.runQuery { QueryDsl.create(tbl_kordlols) }
            db.runQuery { QueryDsl.create(tbl_kords) }
            db.runQuery { QueryDsl.create(tbl_lols) }
            db.runQuery { QueryDsl.create(tbl_matches) }
            db.runQuery { QueryDsl.create(tbl_mmrs) }
            db.runQuery { QueryDsl.create(tbl_heroes) }
            db.runQuery { QueryDsl.create(tbl_participantsnew) }
        }
        if (stockHEROES.bodyReset == null) stockHEROES.bodyReset = { Heroes().getData() }
        if (stockMMR.bodyReset == null) stockMMR.bodyReset = { MMRs().getData() }

        LeagueMainObject.catchHeroNames()
    }

    suspend fun executeProcedure(command: String) {
        printLog("[executeProcedure] $command")
        runQuery { QueryDsl.executeScript(command) }
    }

    suspend fun getHeroFromNameEN(nameEN: String) = stockHEROES.get().find { it.nameEN == nameEN }
    suspend fun getHeroFromKey(key: String) = stockHEROES.get().find { it.key == key }
    suspend fun getMMRforChampion(championName: String) = stockMMR.get().find { it.champion == championName }

    suspend fun getKORDLOLs_forKORD(guilds: Guilds, kord: String) : KORDLOLs? {
        return db.withTransaction {
            db.runQuery {
                QueryDsl.from(tbl_kordlols)
                    .leftJoin(tbl_kords) { tbl_kords.id eq tbl_kordlols.KORD_id }
                    .where { tbl_kords.KORD_id eq kord }
                    .where { tbl_kordlols.guild_id eq guilds.id }
                    .limit(1)
            }.firstOrNull()
        }
    }

    suspend fun getGuild(guild: Guild): Guilds {
        var myGuild = Guilds().getDataOne({ tbl_guilds.idGuild eq guild.id.value.toString() })
        if (myGuild == null) myGuild = Guilds().add(guild)
        return myGuild
    }
}