package ru.descend.bot.postgre.r2dbc

import dev.kord.core.entity.Guild
import io.r2dbc.spi.ConnectionFactoryOptions
import org.komapper.core.dsl.QueryDsl
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

    suspend fun getParticipantsForMatch(match_id: Int) : List<Participants> {
        var value: List<Participants>? = null

        db.withTransaction {
            value = db.runQuery {
                QueryDsl.from(tbl_participants).where {
                    tbl_participants.match_id eq match_id
                }
            }
        }

        return value?: listOf()
    }

    suspend fun getGuild(guild: Guild): Guilds {
        var myGuild: Guilds? = null

        db.withTransaction {
            myGuild = db.runQuery {
                QueryDsl.from(tbl_guilds).where { tbl_guilds.idGuild eq guild.id.value.toString() }.limit(1)
            }.first()
        }

        if (myGuild == null) {
            myGuild = Guilds.add(guild)
        }
        return myGuild!!
    }
}