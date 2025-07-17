package ru.descend.bot.commands

import com.google.gson.Gson
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.TextChannel
import me.jakejmattson.discordkt.arguments.AnyArg
import me.jakejmattson.discordkt.arguments.ChannelArg
import me.jakejmattson.discordkt.arguments.ChoiceArg
import me.jakejmattson.discordkt.arguments.DoubleArg
import me.jakejmattson.discordkt.arguments.IntegerArg
import me.jakejmattson.discordkt.arguments.UserArg
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.util.fullName
import ru.descend.bot.asyncLaunch
import ru.descend.bot.generateAIText
import ru.descend.bot.lowDescriptor
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.datas.create
import ru.descend.bot.datas.delete
import ru.descend.bot.datas.getData
import ru.descend.bot.datas.getDataOne
import ru.descend.bot.datas.getSize
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs.Companion.tbl_kordlols
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.KORDs.Companion.tbl_kords
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.datas.update
import ru.descend.bot.printLog
import ru.descend.bot.launch
import ru.descend.bot.postgre.calculating.Calc_MMR
import ru.descend.bot.postgre.r2dbc.model.Matches.Companion.tbl_matches
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew.Companion.tbl_participantsnew
import ru.descend.bot.sendMessage
import ru.descend.bot.sqlData
import ru.descend.bot.to1Digits
import ru.descend.bot.toNamedFile
import ru.descend.bot.toStringUID
import java.io.File
import java.util.Calendar
import java.util.GregorianCalendar

fun arguments() = commands("Arguments") {
    slash(
        "setBirthdayDate",
        "Ввести дату рождения пользователя (в формате ddmmyyyy, например 03091990)",
        Permissions(Permission.UseApplicationCommands, Permission.ChangeNickname)
    ) {
        execute(UserArg("user", "Пользователь Discord"), AnyArg("date")) {
            val (user, date) = args
            var dateText = date
            val textCommand =
                "[Start command] '$name' from ${author.fullName} with params: 'User'=${user.lowDescriptor()}, 'date=${dateText}'"
            printLog(textCommand)

            if (date.length < 3) {
                respond("Дата $date не может быть менее 4х символов")
                return@execute
            }
            if (date.length == 4) {
                dateText += "1900"
            }
            if (dateText.length != 8) {
                respond("Ошибка преобразования значения в дату: $date. Необходим формат примера 03091990 (3 сентября 1990 года) или без года (0309)")
                return@execute
            }
            date.forEach {
                if (!it.isDigit()) {
                    respond("В введенном значении даты $date содержится недопустимый символ: $it")
                    return@execute
                }
            }

            dateText += "_" + (GregorianCalendar.getInstance().get(Calendar.YEAR) - 1)

            //val localDate = LocalDate.parse(dateText, DateTimeFormatter.ofPattern("ddMMyyyy"))

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            //Берем KORD либо существующий, либо создаём новый. Не важно
            val KORD = KORDs().getDataOne({ tbl_kords.KORD_id eq user.toStringUID(); tbl_kords.guild_id eq guilds.id })
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

    slash("changeShowCode", "Изменить SHOWCODE пользователя", Permissions(Permission.Administrator)) {
        execute(
            AnyArg("currentCode", "Текущий Show code"),
            AnyArg("newCode", "Новый Show code")
        ) {
            val (currentCode, newCode) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'currentCode=$currentCode', 'newCode=$newCode'"
            printLog(textCommand)
            val r2Guild = R2DBC.getGuild(guild)
            guild.sendMessage(r2Guild.messageIdDebug, textCommand)

            if (currentCode.toIntOrNull() == null || newCode.toIntOrNull() == null) {
                respond("Некорректный код currentCode($currentCode) и|или newCode($newCode)")
                return@execute
            }

            val findedOldLOL = LOLs().getDataOne({ tbl_lols.show_code eq currentCode.toInt() })
            if (findedOldLOL == null) {
                respond("Не найден пользователь LOL с showCode $currentCode")
                return@execute
            }

            val findedNewLOL = LOLs().getDataOne({ tbl_lols.show_code eq newCode.toInt() })
            if (findedNewLOL != null) {
                respond("Пользователь с showCode $newCode уже существует: $findedNewLOL")
                return@execute
            }

            findedOldLOL.show_code = newCode.toInt()
            findedOldLOL.update()

            sqlData.isNeedUpdateDays = true
            respond("Успешно изменен showCode у пользователя с '$currentCode' на '$newCode'")
        }
    }

    slash("initMainChannel", "Основной канал для бота", Permissions(Permission.Administrator)) {
        execute(ChannelArg<TextChannel>("channel")) {
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

    slash("clearMainChannel", "Очистка основного канал для бота", Permissions(Permission.Administrator)) {
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

    slash("initStatusChannel", "Канал для сообщений бота", Permissions(Permission.Administrator)) {
        execute(ChannelArg<TextChannel>("channel")) {
            val (channel) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'channel=${channel.name}'"
            val r2Guild = R2DBC.getGuild(guild)
            guild.sendMessage(r2Guild.messageIdDebug, textCommand)
            r2Guild.messageIdStatus = channel.id.value.toString()
            r2Guild.update()
            respond("Guild status channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash("clearStatusChannel", "Очистка канала для сообщений бота", Permissions(Permission.Administrator)) {
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

    slash("initDebugChannel", "Канал для системных сообщений", Permissions(Permission.Administrator)) {
        execute(ChannelArg<TextChannel>("channel")) {
            val (channel) = args
            val textCommand =
                "[Start command] '$name' from ${author.fullName} with params: 'channel=${channel.name}'"
            printLog(textCommand)
            val r2Guild = R2DBC.getGuild(guild)
            guild.sendMessage(r2Guild.messageIdDebug, textCommand)
            r2Guild.messageIdDebug = channel.id.value.toString()
            r2Guild.update()
            respond("Guild status channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash("clearDebugChannel", "Очистка канала для системных сообщений", Permissions(Permission.Administrator)) {
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

    slash("userCreate", "Создание учетной записи Лиги легенд и пользователя Discord", Permissions(Permission.Administrator)) {
        execute(
            UserArg("User", "Пользователь Discord"),
            ChoiceArg("Region", "Регион аккаунта Лиги легенд", "ru", "br1", "eun1", "euw1", "jp1", "kr", "la1", "la2", "na1", "oc1", "tr1", "pbe1"),
            AnyArg("SummonerName", "Имя призывателя в Лиге легенд"),
            AnyArg("TagLine", "Тег игрока")
        ) {
            val (user, region, summonerName, tagLine) = args

            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'user=${user.fullName}', 'region=$region', 'summonerName=$summonerName', 'tagLine=$tagLine'"
            printLog(textCommand)

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            //Берем KORD либо существующий, либо создаём новый. Не важно
            var KORD = KORDs(guilds, user)
            KORD = KORDs().getDataOne({ tbl_kords.KORD_id eq user.toStringUID(); tbl_kords.guild_id eq guilds.id }) ?: KORD.create(KORDs::KORD_id).result

            //Создаем LOL сразу со связью с аккаунтом Лиги Легенд
            var LOL = LOLs().connectLOL(region, summonerName, tagLine)
            if (LOL == null) {
                respond("Призыватель $summonerName#$tagLine не найден в Лиге Легенд")
                return@execute
            }

            //Если аккаунт есть в базе - ок, если нет - создаём в базе
            LOL = LOLs().getDataOne({ tbl_lols.LOL_puuid eq LOL!!.LOL_puuid }) ?: LOL.create(LOLs::LOL_puuid).result
            val allShow = LOLs().getData({ tbl_lols.show_code notEq 0 })

            var index = 0
            while (true) {
                index++
                if (allShow.find { it.show_code == index } == null) break
            }

            LOL.show_code = index
            LOL = LOL.update()

            //Проверка что к пользователю уже привязан какой-либо аккаунт лиги
            val alreadyKORDLOL = KORDLOLs().getDataOne({ tbl_kordlols.KORD_id eq KORD.id; tbl_kordlols.guild_id eq guilds.id; tbl_kordlols.LOL_id eq LOL.id })
            if (alreadyKORDLOL != null) {
                respond("Призыватель уже связан с аккаунтом лиги легенд (KORDLOL: ${alreadyKORDLOL.id}). Для внесения изменений - сначала удалите из базы пользователя ${user.lowDescriptor()}")
                return@execute
            }

            asyncLaunch {
                val KORDLOL = KORDLOLs(
                    KORD_id = KORD.id,
                    LOL_id = LOL.id,
                    guild_id = guilds.id
                )

                KORDLOL.create(null).result
            }
            sqlData.isNeedUpdateDays = true
            respond("Пользователь ${user.lowDescriptor()} успешно связан с учётной записью ${LOL.getCorrectName()}")
        }
    }

    slash("genText", "Получить ответ от ChatGPT на запрос", Permissions(Permission.UseApplicationCommands)) {
        execute(AnyArg("request")) {
            val (request) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'request'=${request}"
            printLog(textCommand)

            asyncLaunch {
                var result = "${author.lowDescriptor()}: $request\n\nОтвет:\n"
                result += generateAIText(request)
                channel.createMessage(result)
            }

            respond("Ожидание ответа...")
        }
    }

    slash("getMatchAramMMR", "Получить статистику по подсчёту ММР по матчу по всем пользователям", Permissions(Permission.Administrator)) {
        execute(AnyArg("matchText")) {
            val (matchText) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'matchText'=${matchText}"
            printLog(textCommand)

            respond("Генерация ответа по матчу $matchText...")

            val match = Matches().getDataOne({ tbl_matches.matchId eq matchText })
            if (match == null) {
                channel.createMessage { content = "Матч '$matchText' не найден в БД бота" }
                return@execute
            }

            val participants = ParticipantsNew().getData({ tbl_participantsnew.match_id eq match.id })
            if (participants.isEmpty()) {
                channel.createMessage { content = "Не найдены игроки в матче '$matchText'" }
                return@execute
            }

            launch {
                val result = Calc_MMR(participants, match)
                result.calculateMMR()
                val file = File("${matchText}_ALL.txt")
                file.writeText(result.mmrValueTextLog)
                channel.createMessage {
                    content = "Файл данных по матчу $matchText"
                    files.add(file.toNamedFile())
                }
                file.delete()
            }
        }
    }

    slash("getMatchInfo", "Получить данные из Бота по указанному матчу", Permissions(Permission.UseApplicationCommands)) {
        execute(AnyArg("matchText")) {
            val (matchText) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'matchText'=${matchText}"
            printLog(textCommand)

            val match = Matches().getDataOne({ tbl_matches.matchId eq matchText })
            if (match == null) {
                respond("Матч '$matchText' не найден в БД бота")
                return@execute
            }

            launch {
                var textAnswer = "Данные по матчу\n"
                textAnswer += Gson().toJson(match)

                textAnswer += "\nДанные по игрокам\n"
                val participants = ParticipantsNew().getData({ tbl_participantsnew.match_id eq match.id })
                participants.forEach {
                    textAnswer += Gson().toJson(it) + "\n"
                }

                val file = File("$matchText.txt")
                file.writeText(textAnswer)
                channel.createMessage {
                    content = "Файл данных по матчу $matchText"
                    files.add(file.toNamedFile())
                }
                file.delete()
            }

            respond("Генерация ответа...")
        }
    }

    slash("genTextAdmin", "", Permissions(Permission.Administrator)) {
        execute(AnyArg("request")) {
            val (request) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'request'=${request}"
            printLog(textCommand)

            asyncLaunch {
                channel.createMessage(generateAIText(request))
            }

            respond("Ожидание ответа...")
        }
    }

    slash("userDeleteFromID", "Удалить учётную запись из базы данных бота по show ID", Permissions(Permission.Administrator)) {
        execute(IntegerArg("id", "show id пользователя Discord")) {
            val (id) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'id=$id'"
            printLog(textCommand)

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            val dataLOL = LOLs().getDataOne({ tbl_lols.show_code eq id })
            if (dataLOL == null) {
                respond("Пользователя с show_code $id в базе не найдено. Операция отменена. Обратитесь к Администратору")
                return@execute
            }
            printLog("[DELETE] dataLOL: $dataLOL")

            val dataKORDLOLs = KORDLOLs().getDataOne({ tbl_kordlols.LOL_id eq dataLOL.id })
            if (dataKORDLOLs == null) {
                respond("Пользователя с id $id в базе не найдено. Операция отменена. Обратитесь к Администратору")
                return@execute
            }
            printLog("[DELETE] dataKORD: $dataKORDLOLs")
            val kordId = dataKORDLOLs.KORD_id
            dataLOL.show_code = 0
            dataLOL.update()

            val textMessage = run {
                dataKORDLOLs.delete()
                "Удаление произошло успешно"
            }

            val dataKORDLOLsDeleted = KORDLOLs().getDataOne({ tbl_kordlols.KORD_id eq kordId })
            if (dataKORDLOLsDeleted == null) {
                printLog("[DELETE KORD]")
                KORDs().getDataOne({ tbl_kords.id eq kordId })?.delete()
            }

            respond(textMessage)
        }
    }

    slash("addSavedMMR", "Добавить бонусные MMR пользователю", Permissions(Permission.Administrator)) {
        execute(UserArg("user", "Пользователь Discord"), IntegerArg("savedMMR", "Количество бонусных MMR")) {
            val (user, savedMMR) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'user=${user.lowDescriptor()}' 'savedMMR=$savedMMR'"
            printLog(textCommand)

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            val dataUser = R2DBC.getKORDLOLs_forKORD(guilds, user.toStringUID())
            val textMessage = if (dataUser == null) {
                "Пользователя не существует в базе"
            } else {
                LOLs().getDataOne({ tbl_lols.id.eq(dataUser.LOL_id) })?.let { lol ->
                    lol.mmrAramSaved += savedMMR.toDouble().to1Digits()
                    lol.update()
                }
                "Сохранение успешно произведено"
            }
            respond(textMessage)
        }
    }

    slash("removeSavedMMR", "Вычесть бонусные MMR пользователю", Permissions(Permission.Administrator)) {
        execute(UserArg("user", "Пользователь Discord"), IntegerArg("savedMMR", "Количество вычитаемых MMR")) {
            val (user, savedMMR) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'user=${user.lowDescriptor()}' 'savedMMR=$savedMMR'"
            printLog(textCommand)

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            val dataUser = R2DBC.getKORDLOLs_forKORD(guilds, user.toStringUID())
            val textMessage = if (dataUser == null) {
                "Пользователя не существует в базе"
            } else {
                LOLs().getDataOne({ tbl_lols.id.eq(dataUser.LOL_id) })?.let { lol ->
                    lol.mmrAramSaved -= savedMMR.toDouble().to1Digits()
                    lol.update()
                }
                "Сохранение успешно произведено"
            }
            respond(textMessage)
        }
    }

    slash("addDonation", "Добавить донат к пользователю", Permissions(Permission.Administrator)) {
        execute(UserArg("user", "Пользователь Discord"), DoubleArg("gold", "Количество денег")) {
            val (user, gold) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'user=${user.lowDescriptor()}' 'gold=$gold'"
            printLog(textCommand)

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            val dataUser = KORDs().getDataOne({ tbl_kords.KORD_id eq user.toStringUID() })
            val textMessage = if (dataUser == null) {
                "Пользователя не существует в базе"
            } else {
                dataUser.donations = (dataUser.donations + gold).to1Digits()
                dataUser.update()
                "Сохранение успешно произведено"
            }
            respond(textMessage)
        }
    }
}