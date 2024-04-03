package ru.descend.bot.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.entity.channel.TextChannel
import me.jakejmattson.discordkt.arguments.*
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.util.fullName
import ru.descend.bot.lowDescriptor
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.create
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs.Companion.tbl_kordlols
import ru.descend.bot.postgre.r2dbc.model.KORDs.Companion.tbl_kords
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.update
import ru.descend.bot.printLog
import ru.descend.bot.sendMessage
import ru.descend.bot.to2Digits
import ru.descend.bot.toStringUID
import java.util.Calendar
import java.util.GregorianCalendar

fun arguments() = commands("Arguments") {

    slash("setBirthdayDate", "Ввести дату рождения пользователя (в формате ddmmyyyy, например 03091990)", Permissions(Permission.UseApplicationCommands)){
        execute(UserArg("user", "Пользователь Discord"), AnyArg("date")){
            val (user, date) = args
            var dateText = date
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'User'=${user.lowDescriptor()}, 'date=${dateText}'"
            printLog(textCommand)

            if (date.length < 3) {
                respond("Дата $date не может быть менее 4х символов")
                return@execute
            }
            if (date.length == 4) {
                dateText += "1900"
            }
            if (dateText.length != 8) {
                respond("Ошибка преобразования значения в дату: $date. Необходим формат примера 03091990 (3 сентября 1990 года)")
                return@execute
            }
            date.forEach {
                if (!it.isDigit()) {
                    respond("В введенном значении даты $date содержится недопустимый символ: $it")
                    return@execute
                }
            }
            dateText += "_" + GregorianCalendar.getInstance().get(Calendar.YEAR)

            //val localDate = LocalDate.parse(dateText, DateTimeFormatter.ofPattern("ddMMyyyy"))

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            //Берем KORD либо существующий, либо создаём новый. Не важно
            var KORD = R2DBC.getKORDs { tbl_kords.KORD_id eq user.toStringUID() ; tbl_kords.guild_id eq guilds.id }.firstOrNull()
            if (KORD == null) {
                respond("Пользователь ${user.lowDescriptor()} не зарегистрирован в боте. Его изменение невозможно")
                return@execute
            } else {
                KORD.date_birthday = dateText
                KORD.update()
            }

            respond("Пользователю ${user.lowDescriptor()} привязана дата $date")
        }
    }

    slash("initMainChannel", "Основной канал для бота", Permissions(Permission.Administrator)){
        execute(ChannelArg<TextChannel>("channel")){
            val (channel) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'channel=${channel.name}'"
            printLog(textCommand)
            val r2Guild = R2DBC.getGuild(guild)
            guild.sendMessage(r2Guild.messageIdDebug, textCommand)
            r2Guild.botChannelId = channel.id.value.toString()
            r2Guild.update()
            respond("Guild channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash("clearMainChannel", "Очистка основного канал для бота", Permissions(Permission.Administrator)){
        execute {
            val textCommand = "[Start command] '$name' from ${author.fullName}"
            printLog(textCommand)
            val r2Guild = R2DBC.getGuild(guild)
            guild.sendMessage(r2Guild.messageIdDebug, textCommand)
            r2Guild.botChannelId = ""
            r2Guild.update()
            respond("Guild MainChannel cleared")
        }
    }

    slash("initStatusChannel", "Канал для сообщений бота", Permissions(Permission.Administrator)){
        execute(ChannelArg<TextChannel>("channel")){
            val (channel) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'channel=${channel.name}'"
            val r2Guild = R2DBC.getGuild(guild)
            guild.sendMessage(r2Guild.messageIdDebug, textCommand)
            r2Guild.messageIdStatus = channel.id.value.toString()
            r2Guild.update()
            respond("Guild status channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash("clearStatusChannel", "Очистка канала для сообщений бота", Permissions(Permission.Administrator)){
        execute {
            val textCommand = "[Start command] '$name' from ${author.fullName}"
            printLog(textCommand)
            val r2Guild = R2DBC.getGuild(guild)
            guild.sendMessage(r2Guild.messageIdDebug, textCommand)
            r2Guild.messageIdStatus = ""
            r2Guild.update()
            respond("Guild StatusChannel cleared")
        }
    }

    slash("initDebugChannel", "Канал для системных сообщений", Permissions(Permission.Administrator)){
        execute(ChannelArg<TextChannel>("channel")){
            val (channel) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'channel=${channel.name}'"
            printLog(textCommand)
            val r2Guild = R2DBC.getGuild(guild)
            guild.sendMessage(r2Guild.messageIdDebug, textCommand)
            r2Guild.messageIdDebug = channel.id.value.toString()
            r2Guild.update()
            respond("Guild status channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash("clearDebugChannel", "Очистка канала для системных сообщений", Permissions(Permission.Administrator)){
        execute {
            val textCommand = "[Start command] '$name' from ${author.fullName}"
            printLog(textCommand)
            val r2Guild = R2DBC.getGuild(guild)
            guild.sendMessage(r2Guild.messageIdDebug, textCommand)
            r2Guild.messageIdDebug = ""
            r2Guild.update()
            respond("Guild DebugChannel cleared")
        }
    }

    slash("userCreate", "Создание учетной записи Лиги легенд и пользователя Discord", Permissions(Permission.Administrator)){
        execute(UserArg("User", "Пользователь Discord"), ChoiceArg("Region", "Регион аккаунта Лиги легенд", "ru", "br1", "eun1", "euw1", "jp1", "kr", "la1", "la2", "na1", "oc1", "tr1", "pbe1"), AnyArg("SummonerName", "Имя призывателя в Лиге легенд")){
            val (user, region, summonerName) = args

            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'user=${user.fullName}', 'region=$region', 'summonerName=$summonerName'"
            printLog(textCommand)

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            //Берем KORD либо существующий, либо создаём новый. Не важно
            var KORD = KORDs(guilds, user)
            KORD = R2DBC.getKORDs { tbl_kords.KORD_id eq user.toStringUID() ; tbl_kords.guild_id eq guilds.id }.firstOrNull() ?: KORD.create(KORDs::KORD_id)

            //Создаем LOL сразу со связью с аккаунтом Лиги Легенд
            var LOL = LOLs().connectLOL(region, summonerName)
            if (LOL == null){
                respond("Призыватель $summonerName не найден в Лиге Легенд")
                return@execute
            }

            //Если аккаунт есть в базе - ок, если нет - создаём в базе
            LOL = R2DBC.getLOLs { tbl_lols.LOL_puuid eq LOL!!.LOL_puuid }.firstOrNull() ?: LOL.create(LOLs::LOL_puuid)

            //Проверка что к пользователю уже привязан какой-либо аккаунт лиги
            val alreadyKORDLOL = R2DBC.getKORDLOLs { tbl_kordlols.KORD_id eq KORD.id ; tbl_kordlols.guild_id eq guilds.id }.firstOrNull()
            if (alreadyKORDLOL != null) {
                respond("Призыватель $summonerName уже связан с аккаунтом лиги легенд (KORDLOL: ${alreadyKORDLOL.id}). Для внесения изменений - сначала удалите из базы пользователя ${user.lowDescriptor()}")
                return@execute
            }

            val KORDLOL = KORDLOLs(
                KORD_id = KORD.id,
                LOL_id = LOL.id,
                guild_id = guilds.id
            )
            KORDLOL.create(KORDLOLs::KORD_id)

            respond("Пользователь ${user.lowDescriptor()} успешно связан с учётной записью ${LOL.LOL_summonerName}")
        }
    }

    slash("userDelete", "Удалить учётную запись из базы данных бота", Permissions(Permission.Administrator)){
        execute(UserArg("User", "Пользователь Discord")){
            val (user) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'user=${user.fullName}'"
            printLog(textCommand)

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            val dataKORD = R2DBC.getKORDs { tbl_kords.KORD_id eq user.toStringUID() ; tbl_kords.guild_id eq guilds.id }.firstOrNull()
            val textMessage = if (dataKORD == null) {
                "Пользователя не существует в базе"
            } else {
                dataKORD.deleteWithKORDLOL(guilds)
                "Удаление произошло успешно"
            }
            respond(textMessage)
        }
    }

    slash("addSavedMMR", "Добавить бонусные MMR пользователю", Permissions(Permission.Administrator)){
        execute(UserArg("user", "Пользователь Discord"), IntegerArg("savedMMR", "Количество бонусных MMR")){
            val (user, savedMMR) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'user=$user' 'savedMMR=$savedMMR'"
            printLog(textCommand)

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            val dataUser = R2DBC.getKORDLOLs_forKORD(guilds, user.toStringUID())
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
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'user=$user' 'savedMMR=$savedMMR'"
            printLog(textCommand)

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            val dataUser = R2DBC.getKORDLOLs_forKORD(guilds, user.toStringUID())
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
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'user=$UserId'"
            printLog(textCommand)

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            val kordlol = R2DBC.getKORDLOLs { tbl_kordlols.id eq UserId ; tbl_kordlols.guild_id eq guilds.id }.firstOrNull()
            val textMessage = if (kordlol != null) {
                kordlol.deleteWithKORD(guilds)
                "Удаление прошло успешно"
            } else {
                "Пользователя с ID $UserId не существует"
            }

            respond(textMessage)
        }
    }
}