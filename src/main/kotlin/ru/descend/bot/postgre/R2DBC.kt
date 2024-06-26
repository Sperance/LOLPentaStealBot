package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import io.r2dbc.spi.ConnectionFactoryOptions
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.expression.ScalarExpression
import org.komapper.core.dsl.expression.SortExpression
import org.komapper.core.dsl.expression.WhereDeclaration
import org.komapper.core.dsl.operator.count
import org.komapper.core.dsl.operator.desc
import org.komapper.core.dsl.query.Query
import org.komapper.core.dsl.query.QueryScope
import org.komapper.core.dsl.query.firstOrNull
import org.komapper.r2dbc.R2dbcDatabase
import org.komapper.tx.core.CoroutineTransactionOperator
import ru.descend.bot.datas.WorkData
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
        .option(ConnectionFactoryOptions.DATABASE, "postgres2")
        .build()

    private val db = R2dbcDatabase(connectionFactory)

    val stockHEROES = WorkData<Heroes>("HEROES")

    suspend fun runTransaction(body: suspend CoroutineTransactionOperator.() -> Unit) = db.withTransaction { body.invoke(it) }

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
            db.runQuery { QueryDsl.create(tbl_heroes) }
        }
        if (stockHEROES.bodyReset == null) stockHEROES.bodyReset = { getHeroes() }

        LeagueMainObject.catchHeroNames()
    }

    suspend fun executeProcedure(command: String) {
        printLog("[executeProcedure] $command")
        runQuery {
            QueryDsl.executeScript(command)
        }
    }

    private suspend fun getGuilds(declaration: WhereDeclaration?) : List<Guilds> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_guilds) else QueryDsl.from(tbl_guilds).where(declaration) } }
    }

    suspend fun getKORDLOLs(declaration: WhereDeclaration?) : List<KORDLOLs> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_kordlols) else QueryDsl.from(tbl_kordlols).where(declaration) } }
    }

    suspend fun getKORDLOLone(declaration: WhereDeclaration = { tbl_kordlols.id greaterEq 0}, first: Boolean = true) : KORDLOLs? {
        val sortExpr = if (first) tbl_kordlols.id else tbl_kordlols.id.desc()
        val query = QueryDsl.from(tbl_kordlols)
            .where(declaration)
            .orderBy(sortExpr)
            .firstOrNull()
        return db.withTransaction { db.runQuery { query } }
    }

    suspend fun getMMRs(declaration: WhereDeclaration?) : List<MMRs> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_mmrs) else QueryDsl.from(tbl_mmrs).where(declaration) } }
    }

    suspend fun getParticipantOne(declaration: WhereDeclaration = { tbl_participants.id greaterEq 0}, first: Boolean = true) : Participants? {
        val sortExpr = if (first) tbl_participants.id else tbl_participants.id.desc()
        val query = QueryDsl.from(tbl_participants)
            .where(declaration)
            .orderBy(sortExpr)
            .firstOrNull()
        return db.withTransaction { db.runQuery { query } }
    }
    suspend fun getParticipants(declaration: WhereDeclaration?) : List<Participants> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_participants) else QueryDsl.from(tbl_participants).where(declaration) } }
    }
    suspend fun getParticipantsSize(declaration: WhereDeclaration) : Long {
        val query = QueryDsl.from(tbl_participants)
            .where(declaration)
            .select(count())
        return db.withTransaction { db.runQuery { query } }?:0L
    }
    suspend fun addBatchParticipants(list: List<Participants>, batchSize: Int = 100) : List<Participants> {
        return db.withTransaction {
            val miniList = ArrayList<Participants>()
            val resultedList = ArrayList<Participants>()
            list.forEach { value ->
                miniList.add(value)
                if (miniList.size == batchSize) {
                    val res = db.runQuery { QueryDsl.insert(tbl_participants).multiple(miniList) }
                    resultedList.addAll(res)
                    res.forEach { printLog("\t[Batch_Participants::save] $it", false) }
                    miniList.clear()
                }
            }
            if (miniList.isNotEmpty()) {
                val res = db.runQuery { QueryDsl.insert(tbl_participants).multiple(miniList) }
                resultedList.addAll(res)
                res.forEach { printLog("\t[Batch_Participants::save] $it", false) }
            }
            resultedList
        }
    }

    suspend fun getHeroesone(declaration: WhereDeclaration = { tbl_heroes.id greaterEq 0}, first: Boolean = true) : Heroes? {
        val sortExpr = if (first) tbl_heroes.id else tbl_heroes.id.desc()
        val query = QueryDsl.from(tbl_heroes)
            .where(declaration)
            .orderBy(sortExpr)
            .firstOrNull()
        return db.withTransaction { db.runQuery { query } }
    }
    suspend fun getHeroes(declaration: WhereDeclaration? = null) : List<Heroes> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_heroes) else QueryDsl.from(tbl_heroes).where(declaration) } }
    }

    suspend fun getLOLone(declaration: WhereDeclaration = { tbl_lols.id greaterEq 0}, sortExpression: SortExpression = tbl_lols.id) : LOLs? {
        val query = QueryDsl.from(tbl_lols)
            .where(declaration)
            .orderBy(sortExpression)
            .limit(1)
            .firstOrNull()
        return db.withTransaction { db.runQuery { query } }
    }
    suspend fun getLOLSize(declaration: WhereDeclaration) : Long {
        val query = QueryDsl.from(tbl_lols)
            .where(declaration)
            .select(count())
        return db.withTransaction { db.runQuery { query } }?:0L
    }
    suspend fun getLOLs(declaration: WhereDeclaration) : List<LOLs> {
        return getLOLs(declaration, tbl_lols.id, null)
    }
    suspend fun getLOLs(declaration: WhereDeclaration = { tbl_lols.id greaterEq 0}, sortExpression: SortExpression = tbl_lols.id, limit: Int? = null) : List<LOLs> {
        return db.withTransaction {
            db.runQuery {
                if (limit == null) QueryDsl.from(tbl_lols).orderBy(sortExpression).where(declaration) else QueryDsl.from(tbl_lols).orderBy(sortExpression).where(declaration).limit(limit)
            }
        }
    }
    suspend fun addBatchLOLs(list: List<LOLs>, batchSize: Int = 100) {
        db.withTransaction {
            val miniList = ArrayList<LOLs>()
            list.forEach { value ->
                miniList.add(value)
                if (miniList.size == batchSize) {
                    val res = db.runQuery { QueryDsl.insert(tbl_lols).multiple(miniList) }
                    res.forEach { printLog("\t[Batch_LOLs::save] $it") }
                    miniList.clear()
                }
            }
            if (miniList.isNotEmpty()) {
                val res = db.runQuery { QueryDsl.insert(tbl_lols).multiple(miniList) }
                res.forEach { printLog("\t[Batch_LOLs::save] $it") }
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

    suspend fun getMatchOne(declaration: WhereDeclaration = { tbl_matches.id greaterEq 0}, sortExpression: SortExpression = tbl_matches.id) : Matches? {
        val query = QueryDsl.from(tbl_matches)
            .where(declaration)
            .orderBy(sortExpression)
            .limit(1)
            .firstOrNull()
        return db.withTransaction { db.runQuery { query } }
    }
    suspend fun getMatches(declaration: WhereDeclaration?) : List<Matches> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_matches) else QueryDsl.from(tbl_matches).where(declaration) } }
    }
    suspend fun getMatchesSize(declaration: WhereDeclaration) : Long {
        val query = QueryDsl.from(tbl_matches)
            .where(declaration)
            .select(count())
        return db.withTransaction { db.runQuery { query } }?:0L
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

    suspend fun getHeroFromNameEN(nameEN: String) = getHeroesone({ tbl_heroes.nameEN eq nameEN})
    suspend fun getHeroFromKey(key: String) = getHeroesone({ tbl_heroes.key eq key})

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