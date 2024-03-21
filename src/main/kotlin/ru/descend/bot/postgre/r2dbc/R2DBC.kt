package ru.descend.bot.postgre.r2dbc

import dev.kord.core.entity.Guild
import io.r2dbc.spi.ConnectionFactoryOptions
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.expression.WhereDeclaration
import org.komapper.r2dbc.R2dbcDatabase
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.model.tbl_KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_KORDs
import ru.descend.bot.postgre.r2dbc.model.tbl_LOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_MMRs
import ru.descend.bot.postgre.r2dbc.model.tbl_guilds
import ru.descend.bot.postgre.r2dbc.model.tbl_matches
import ru.descend.bot.postgre.r2dbc.model.tbl_participants

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

    val db = R2dbcDatabase(connectionFactory)

    suspend fun initialize() {
        db.withTransaction {
            db.runQuery {
                QueryDsl.create(tbl_guilds)
            }
            db.runQuery {
                QueryDsl.create(tbl_KORDLOLs)
            }
            db.runQuery {
                QueryDsl.create(tbl_KORDs)
            }
            db.runQuery {
                QueryDsl.create(tbl_LOLs)
            }
            db.runQuery {
                QueryDsl.create(tbl_matches)
            }
            db.runQuery {
                QueryDsl.create(tbl_MMRs)
            }
            db.runQuery {
                QueryDsl.create(tbl_participants)
            }
        }
    }

    suspend fun getGuilds(declaration: WhereDeclaration?) : List<Guilds> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_guilds) else QueryDsl.from(tbl_guilds).where(declaration) } }
    }

    suspend fun getKORDLOLs(declaration: WhereDeclaration?) : List<KORDLOLs> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_KORDLOLs) else QueryDsl.from(tbl_KORDLOLs).where(declaration) } }
    }

    suspend fun getMMRs(declaration: WhereDeclaration?) : List<MMRs> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_MMRs) else QueryDsl.from(tbl_MMRs).where(declaration) } }
    }

    suspend fun getParticipants(declaration: WhereDeclaration?) : List<Participants> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_participants) else QueryDsl.from(tbl_participants).where(declaration) } }
    }

    suspend fun getLOLs(declaration: WhereDeclaration?) : List<LOLs> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_LOLs) else QueryDsl.from(tbl_LOLs).where(declaration) } }
    }

    suspend fun getKORDs(declaration: WhereDeclaration?) : List<KORDs> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_KORDs) else QueryDsl.from(tbl_KORDs).where(declaration) } }
    }

    suspend fun getMatches(declaration: WhereDeclaration?) : List<Matches> {
        return db.withTransaction { db.runQuery { if (declaration == null) QueryDsl.from(tbl_matches) else QueryDsl.from(tbl_matches).where(declaration) } }
    }

    suspend fun getKORDLOLs_forKORD(kord: String) : KORDLOLs? {
        return db.withTransaction {
            db.runQuery {
                QueryDsl.from(tbl_KORDLOLs)
                    .leftJoin(tbl_KORDs) { tbl_KORDs.id eq tbl_KORDLOLs.KORD_id }
                    .where { tbl_KORDs.KORD_id eq kord }
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