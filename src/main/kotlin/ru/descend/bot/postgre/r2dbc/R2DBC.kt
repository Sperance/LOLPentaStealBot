package ru.descend.bot.postgre.r2dbc

import dev.kord.core.entity.Guild
import io.r2dbc.spi.ConnectionFactoryOptions
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.r2dbc.R2dbcDatabase
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.model.guilds
import ru.descend.bot.postgre.r2dbc.model.korDs
import ru.descend.bot.postgre.r2dbc.model.kordloLs
import ru.descend.bot.postgre.r2dbc.model.loLs
import ru.descend.bot.postgre.r2dbc.model.matches
import ru.descend.bot.postgre.r2dbc.model.mmRs
import ru.descend.bot.postgre.r2dbc.model.participants
import ru.descend.bot.postgre.tables.TableGuild
import ru.descend.bot.postgre.tables.tableGuild
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

    val tbl_participants = Meta.participants
    val tbl_matches = Meta.matches
    val tbl_guilds = Meta.guilds
    val tbl_mmrs = Meta.mmRs
    val tbl_lols = Meta.loLs
    val tbl_kords = Meta.korDs
    val tbl_kordlols = Meta.kordloLs
    val db = R2dbcDatabase(connectionFactory)

    suspend fun initialize() {
        db.withTransaction {
            db.runQuery {
                QueryDsl.create(tbl_participants)
            }
            db.runQuery {
                QueryDsl.create(tbl_matches)
            }
            db.runQuery {
                QueryDsl.create(tbl_guilds)
            }
            db.runQuery {
                QueryDsl.create(tbl_mmrs)
            }
            db.runQuery {
                QueryDsl.create(tbl_lols)
            }
            db.runQuery {
                QueryDsl.create(tbl_kords)
            }
            db.runQuery {
                QueryDsl.create(tbl_kordlols)
            }
        }
    }

    suspend fun getGuild(guild: Guild): Guilds? {
        var myGuild: Guilds? = null

        db.withTransaction {
            myGuild = db.runQuery {
                QueryDsl.from(tbl_guilds).where { tbl_guilds.idGuild eq guild.id.value.toString() }.limit(1)
            }.first()
        }

        if (myGuild == null) {
            myGuild = Guilds.addGuild(guild)
        }
        return myGuild
    }

    suspend fun getLOLforPUUID(puuid: String): LOLs? {
        var value: LOLs? = null

        db.withTransaction {
            value = db.runQuery {
                QueryDsl.from(tbl_lols).where { tbl_lols.LOL_puuid eq puuid }.limit(1)
            }.first()
        }

        return value
    }
}