package ru.descend.bot.postgre.r2dbc

import dev.kord.core.entity.Guild
import io.r2dbc.spi.ConnectionFactoryOptions
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.expression.WhereDeclaration
import org.komapper.core.dsl.query.Query
import org.komapper.core.dsl.query.QueryScope
import org.komapper.r2dbc.R2dbcDatabase
import org.komapper.tx.core.CoroutineTransactionOperator
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.Guilds.Companion.tbl_guilds
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
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.model.Participants.Companion.tbl_participants
import ru.descend.bot.printLog

/**
 * https://www.komapper.org/docs/
 */
object R2DBC {

    private val connectionFactory: ConnectionFactoryOptions = ConnectionFactoryOptions.builder()
        .option(ConnectionFactoryOptions.DRIVER, "postgresql")
        .option(ConnectionFactoryOptions.HOST, "localhost")
        .option(ConnectionFactoryOptions.PORT, 5432)
        .option(ConnectionFactoryOptions.USER, "postgres")
        .option(ConnectionFactoryOptions.PASSWORD, "22322137")
        .option(ConnectionFactoryOptions.DATABASE, "postgres")
        .build()

    private val db = R2dbcDatabase(connectionFactory)

    suspend fun <T> runQuery(query: Query<T>) = db.withTransaction { db.runQuery { query } }
    suspend fun <T> runQuery(block: QueryScope.() -> Query<T>) =
        db.withTransaction {
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
            db.runQuery { QueryDsl.create(tbl_participants) }
        }
    }

    private suspend fun getGuilds(declaration: WhereDeclaration?) : List<Guilds> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_guilds) else QueryDsl.from(tbl_guilds).where(declaration) } }
    }

    suspend fun getKORDLOLs(declaration: WhereDeclaration?) : List<KORDLOLs> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_kordlols) else QueryDsl.from(tbl_kordlols).where(declaration) } }
    }
    suspend fun addBatchKORDLOLs(list: List<KORDLOLs>, batchSize: Int = 100) {
        db.withTransaction {
            val miniList = ArrayList<KORDLOLs>()
            list.forEach { value ->
                miniList.add(value)
                if (miniList.size == batchSize) {
                    val res = db.runQuery { QueryDsl.insert(tbl_kordlols).multiple(miniList) }
                    res.forEach { printLog("[Batch_KORDLOLs::save] $it") }
                    miniList.clear()
                }
            }
            if (miniList.isNotEmpty()) {
                val res = db.runQuery { QueryDsl.insert(tbl_kordlols).multiple(miniList) }
                res.forEach { printLog("[Batch_KORDLOLs::save] $it") }
            }
        }
    }

    suspend fun getMMRs(declaration: WhereDeclaration?) : List<MMRs> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_mmrs) else QueryDsl.from(tbl_mmrs).where(declaration) } }
    }
    suspend fun addBatchMMRs(list: List<MMRs>, batchSize: Int = 100) {
        db.withTransaction {
            val miniList = ArrayList<MMRs>()
            list.forEach { value ->
                miniList.add(value)
                if (miniList.size == batchSize) {
                    val res = db.runQuery { QueryDsl.insert(tbl_mmrs).multiple(miniList) }
                    res.forEach { printLog("[Batch_MMRs::save] $it") }
                    miniList.clear()
                }
            }
            if (miniList.isNotEmpty()) {
                val res = db.runQuery { QueryDsl.insert(tbl_mmrs).multiple(miniList) }
                res.forEach { printLog("[Batch_MMRs::save] $it") }
            }
        }
    }

    suspend fun getParticipants(declaration: WhereDeclaration?) : List<Participants> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_participants) else QueryDsl.from(tbl_participants).where(declaration) } }
    }
    suspend fun addBatchParticipants(list: List<Participants>, batchSize: Int = 100) {
        db.withTransaction {
            val miniList = ArrayList<Participants>()
            list.forEach { value ->
                miniList.add(value)
                if (miniList.size == batchSize) {
                    val res = db.runQuery { QueryDsl.insert(tbl_participants).multiple(miniList) }
                    res.forEach { printLog("[Batch_Participants::save] $it") }
                    miniList.clear()
                }
            }
            if (miniList.isNotEmpty()) {
                val res = db.runQuery { QueryDsl.insert(tbl_participants).multiple(miniList) }
                res.forEach { printLog("[Batch_Participants::save] $it") }
            }
        }
    }

    suspend fun getLOLs(declaration: WhereDeclaration?) : List<LOLs> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_lols) else QueryDsl.from(tbl_lols).where(declaration) } }
    }
    suspend fun addBatchLOLs(list: List<LOLs>, batchSize: Int = 100) {
        db.withTransaction {
            val miniList = ArrayList<LOLs>()
            list.forEach { value ->
                miniList.add(value)
                if (miniList.size == batchSize) {
                    val res = db.runQuery { QueryDsl.insert(tbl_lols).multiple(miniList) }
                    res.forEach { printLog("[Batch_LOLs::save] $it") }
                    miniList.clear()
                }
            }
            if (miniList.isNotEmpty()) {
                val res = db.runQuery { QueryDsl.insert(tbl_lols).multiple(miniList) }
                res.forEach { printLog("[Batch_LOLs::save] $it") }
            }
        }
    }

    suspend fun getKORDs(declaration: WhereDeclaration?) : List<KORDs> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_kords) else QueryDsl.from(tbl_kords).where(declaration) } }
    }
    suspend fun addBatchKORDs(list: List<KORDs>, batchSize: Int = 100) {
        db.withTransaction {
            val miniList = ArrayList<KORDs>()
            list.forEach { value ->
                miniList.add(value)
                if (miniList.size == batchSize) {
                    val res = db.runQuery { QueryDsl.insert(tbl_kords).multiple(miniList) }
                    res.forEach { printLog("[Batch_KORDs::save] $it") }
                    miniList.clear()
                }
            }
            if (miniList.isNotEmpty()) {
                val res = db.runQuery { QueryDsl.insert(tbl_kords).multiple(miniList) }
                res.forEach { printLog("[Batch_KORDs::save] $it") }
            }
        }
    }

    suspend fun getMatches(declaration: WhereDeclaration?) : List<Matches> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_matches) else QueryDsl.from(tbl_matches).where(declaration) } }
    }
    suspend fun addBatchMatches(list: List<Matches>, batchSize: Int = 100) {
        db.withTransaction {
            val miniList = ArrayList<Matches>()
            list.forEach { value ->
                miniList.add(value)
                if (miniList.size == batchSize) {
                    val res = db.runQuery { QueryDsl.insert(tbl_matches).multiple(miniList) }
                    res.forEach { printLog("[Batch_Matches::save] $it") }
                    miniList.clear()
                }
            }
            if (miniList.isNotEmpty()) {
                val res = db.runQuery { QueryDsl.insert(tbl_matches).multiple(miniList) }
                res.forEach { printLog("[Batch_Matches::save] $it") }
            }
        }
    }

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
        var myGuild = getGuilds { tbl_guilds.idGuild eq guild.id.value.toString() }.firstOrNull()
        if (myGuild == null) myGuild = Guilds().add(guild)
        return myGuild
    }
}