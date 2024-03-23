package ru.descend.bot.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.entity.channel.TextChannel
import me.jakejmattson.discordkt.arguments.*
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.extensions.descriptor
import me.jakejmattson.discordkt.extensions.fullName
import ru.descend.bot.lowDescriptor
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_KORDs
import ru.descend.bot.postgre.r2dbc.model.tbl_LOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_guilds
import ru.descend.bot.printLog
import ru.descend.bot.to2Digits
import ru.descend.bot.toStringUID

fun arguments() = commands("Arguments") {

    slash("initMainChannel", "Основной канал для бота", Permissions(Permission.Administrator)){
        execute(ChannelArg<TextChannel>("channel")){
            val (channel) = args
            printLog("[Start command] '$name' from ${author.fullName} with params: 'channel=${channel.name}'")
            val r2Guild = R2DBC.getGuild(guild)
            r2Guild.botChannelId = channel.id.value.toString()
            r2Guild.update()
            respond("Guild channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash("clearMainChannel", "Очистка основного канал для бота", Permissions(Permission.Administrator)){
        execute {
            printLog("[Start command] '$name' from ${author.fullName}")
            val r2Guild = R2DBC.getGuild(guild)
            r2Guild.botChannelId = ""
            r2Guild.update()
            respond("Guild MainChannel cleared")
        }
    }

    slash("initStatusChannel", "Канал для сообщений бота", Permissions(Permission.Administrator)){
        execute(ChannelArg<TextChannel>("channel")){
            val (channel) = args
            printLog("[Start command] '$name' from ${author.fullName} with params: 'channel=${channel.name}'")
            val r2Guild = R2DBC.getGuild(guild)
            r2Guild.messageIdStatus = channel.id.value.toString()
            r2Guild.update()
            respond("Guild status channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash("clearStatusChannel", "Очистка канала для сообщений бота", Permissions(Permission.Administrator)){
        execute {
            printLog("[Start command] '$name' from ${author.fullName}")
            val r2Guild = R2DBC.getGuild(guild)
            r2Guild.messageIdStatus = ""
            r2Guild.update()
            respond("Guild StatusChannel cleared")
        }
    }

    slash("initDebugChannel", "Канал для системных сообщений", Permissions(Permission.Administrator)){
        execute(ChannelArg<TextChannel>("channel")){
            val (channel) = args
            printLog("[Start command] '$name' from ${author.fullName} with params: 'channel=${channel.name}'")
            val r2Guild = R2DBC.getGuild(guild)
            r2Guild.messageIdDebug = channel.id.value.toString()
            r2Guild.update()
            respond("Guild status channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash("clearDebugChannel", "Очистка канала для системных сообщений", Permissions(Permission.Administrator)){
        execute {
            printLog("[Start command] '$name' from ${author.fullName}")
            val r2Guild = R2DBC.getGuild(guild)
            r2Guild.messageIdDebug = ""
            r2Guild.update()
            respond("Guild DebugChannel cleared")
        }
    }

    slash("userCreate", "Создание учетной записи Лиги легенд и пользователя Discord", Permissions(Permission.Administrator)){
        execute(UserArg("User", "Пользователь Discord"), ChoiceArg("Region", "Регион аккаунта Лиги легенд", "ru", "br1", "eun1", "euw1", "jp1", "kr", "la1", "la2", "na1", "oc1", "tr1", "pbe1"), AnyArg("SummonerName", "Имя призывателя в Лиге легенд")){
            val (user, region, summonerName) = args

            printLog("[Start command] '$name' from ${author.fullName} with params: 'user=${user.fullName}', 'region=$region', 'summonerName=$summonerName'")

            val guild = R2DBC.getGuild(guild)

            //Берем KORD либо существующий, либо создаём новый. Не важно
            var KORD = KORDs(guild, user)
            KORD = R2DBC.getKORDs { tbl_KORDs.KORD_id eq user.toStringUID() ; tbl_KORDs.guild_id eq guild.id }.firstOrNull() ?: KORD.save()

            //Создаем LOL сразу со связью с аккаунтом Лиги Легенд
            var LOL = LOLs().connectLOL(region, summonerName)
            if (LOL == null){
                respond("Призыватель $summonerName не найден в Лиге Легенд")
                return@execute
            }

            //Если аккаунт есть в базе - ок, если нет - создаём в базе
            LOL = R2DBC.getLOLs { tbl_LOLs.LOL_puuid eq LOL!!.LOL_puuid }.firstOrNull() ?: LOL.save()

            //Проверка что к пользователю уже привязан какой-либо аккаунт лиги
            val alreadyKORDLOL = R2DBC.getKORDLOLs { tbl_KORDLOLs.KORD_id eq KORD.id ; tbl_KORDLOLs.guild_id eq guild.id }.firstOrNull()
            if (alreadyKORDLOL != null) {
                respond("Призыватель $summonerName уже связан с аккаунтом лиги легенд (KORDLOL: ${alreadyKORDLOL.id}). Для внесения изменений - сначала удалите из базы пользователя ${user.lowDescriptor()}")
                return@execute
            }

            val KORDLOL = KORDLOLs(
                KORD_id = KORD.id,
                LOL_id = LOL.id,
                guild_id = guild.id
            )
            KORDLOL.save()

            respond("Пользователь ${user.lowDescriptor()} успешно связан с учётной записью ${LOL.LOL_summonerName}")
        }
    }

    slash("userDelete", "Удалить учётную запись из базы данных бота", Permissions(Permission.Administrator)){
        execute(UserArg("User", "Пользователь Discord")){
            val (user) = args
            printLog("[Start command] '$name' from ${author.fullName} with params: 'user=${user.fullName}'")

            val guild = R2DBC.getGuild(guild)

            val dataKORD = R2DBC.getKORDs { tbl_KORDs.KORD_id eq user.toStringUID() ; tbl_KORDs.guild_id eq guild.id }.firstOrNull()
            val textMessage = if (dataKORD == null) {
                "Пользователя не существует в базе"
            } else {
                dataKORD.deleteWithKORDLOL(guild)
                "Удаление произошло успешно"
            }
            respond(textMessage)
        }
    }

    slash("addSavedMMR", "Добавить бонусные MMR пользователю", Permissions(Permission.Administrator)){
        execute(UserArg("user", "Пользователь Discord"), IntegerArg("savedMMR", "Количество бонусных MMR")){
            val (user, savedMMR) = args
            printLog("[Start command] '$name' from ${author.fullName} with params: 'user=$user' 'savedMMR=$savedMMR'")

            val guild = R2DBC.getGuild(guild)

            val dataUser = R2DBC.getKORDLOLs_forKORD(guild, user.toStringUID())
            val textMessage = if (dataUser == null){
                "Пользователя не существует в базе"
            } else {
                dataUser.mmrAramSaved += savedMMR.toDouble().to2Digits()
                dataUser.update()
                "Сохранение успешно произведено"
            }
            respond(textMessage)
        }
    }

    slash("removeSavedMMR", "Вычесть бонусные MMR пользователю", Permissions(Permission.Administrator)){
        execute(UserArg("user", "Пользователь Discord"), IntegerArg("savedMMR", "Количество вычитаемых MMR")){
            val (user, savedMMR) = args
            printLog("[Start command] '$name' from ${author.fullName} with params: 'user=$user' 'savedMMR=$savedMMR'")

            val guild = R2DBC.getGuild(guild)

            val dataUser = R2DBC.getKORDLOLs_forKORD(guild, user.toStringUID())
            val textMessage = if (dataUser == null){
                "Пользователя не существует в базе"
            } else {
                dataUser.mmrAramSaved -= savedMMR.toDouble().to2Digits()
                dataUser.update()
                "Сохранение успешно произведено"
            }
            respond(textMessage)
        }
    }

    slash("userDeleteFromId", "Удалить учётную запись из базы данных бота по ID пользователя", Permissions(Permission.Administrator)){
        execute(IntegerArg("UserId", "Пользователь Discord ID в базе")){
            val (UserId) = args
            printLog("[Start command] '$name' from ${author.fullName} with params: 'user=$UserId'")

            val guild = R2DBC.getGuild(guild)

            val kordlol = R2DBC.getKORDLOLs { tbl_KORDLOLs.id eq UserId ; tbl_KORDLOLs.guild_id eq guild.id }.firstOrNull()
            val textMessage = if (kordlol != null) {
                kordlol.deleteWithKORD(guild)
                "Удаление прошло успешно"
            } else {
                "Пользователя с ID $UserId не существует"
            }

            respond(textMessage)
        }
    }

//    slash("pLoadMatches", "Прогрузить очередные 100 игр пользователя", Permissions(Permission.Administrator)){
//        execute(UserArg("User", "Пользователь Discord"), IntegerArg("startIndex", "Начиная с какой игры (с последней) прогружать матчи")){
//            val (user, startIndex) = args
//            printLog("[Start command] '$name' from ${author.fullName} with params: 'user=${user.fullName}' 'startIndex=$startIndex'")
//
//            val sqlData = SQLData(guild, getGuild(guild))
//            val dataUser = TableKORD_LOL.getForKORD(user)
//            val textMessage = if (dataUser == null){
//                "Пользователя не существует в базе"
//            } else {
//                asyncLaunch {
//                    dataUser.second.forEach {
//                        if (it.LOLperson == null) return@forEach
//                        reloadMatch(sqlData, it.LOLperson!!.LOL_puuid, startIndex)
//                    }
//                    guild.sendMessage(getGuild(guild).messageIdDebug, "Прогрузка матчей (с $startIndex в количестве 100) для пользователя ${user.lowDescriptor()} завершена")
//                }
//                "Прогрузка успешно запущена"
//            }
//
//            respond(textMessage)
//        }
//    }
}