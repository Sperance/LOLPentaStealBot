package ru.descend.bot.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.entity.channel.TextChannel
import me.jakejmattson.discordkt.arguments.AnyArg
import me.jakejmattson.discordkt.arguments.AutocompleteArg
import me.jakejmattson.discordkt.arguments.AutocompleteData
import me.jakejmattson.discordkt.arguments.ChannelArg
import me.jakejmattson.discordkt.arguments.ChoiceArg
import me.jakejmattson.discordkt.arguments.DoubleArg
import me.jakejmattson.discordkt.arguments.IntegerArg
import me.jakejmattson.discordkt.arguments.PrimitiveArgument
import me.jakejmattson.discordkt.arguments.StringArgument
import me.jakejmattson.discordkt.arguments.UserArg
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.util.fullName
import ru.descend.bot.asyncLaunch
import ru.descend.bot.datas.DataStatRate
import ru.descend.bot.generateAIText
import ru.descend.bot.lolapi.LeagueMainObject.catchHeroForName
import ru.descend.bot.lowDescriptor
import ru.descend.bot.mapMainData
import ru.descend.bot.postgre.PostgreTest
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.datas.create
import ru.descend.bot.datas.delete
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs.Companion.tbl_kordlols
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.KORDs.Companion.tbl_kords
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.datas.update
import ru.descend.bot.printLog
import ru.descend.bot.datas.toDate
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.r2dbc.model.Heroes.Companion.tbl_heroes
import ru.descend.bot.sendMessage
import ru.descend.bot.to1Digits
import ru.descend.bot.toStringUID
import java.time.LocalDate
import java.util.Calendar
import java.util.GregorianCalendar

fun arguments() = commands("Arguments") {

    slash(
        "getChampionsWinrate",
        "Просмотр Винрейта по всем своим сыгранным чемпионам за последние 30 дней"
    ) {
        execute {

            val textCommand = "[Start command] '$name' from ${author.fullName}"
            printLog(textCommand)

            respond("Генерация ответа...")

            R2DBC.runTransaction {
                val guilds = R2DBC.getGuild(guild)
                guild.sendMessage(guilds.messageIdDebug, textCommand)

                val KORD = R2DBC.getKORDs { tbl_kords.KORD_id eq author.toStringUID(); tbl_kords.guild_id eq guilds.id }.firstOrNull()
                if (KORD == null) {
                    respond("Вы ${author.lowDescriptor()} не зарегистрированы в боте. Обратитесь к Администратору")
                    return@runTransaction
                }
                val KORDLOL = R2DBC.getKORDLOLs { tbl_kordlols.KORD_id eq KORD.id; tbl_kordlols.guild_id eq guilds.id }.firstOrNull()
                if (KORDLOL == null) {
                    respond("Вы ${author.lowDescriptor()} не привязаны к аккаунту Лиги Легенд. Обратитесь к Администратору")
                    return@runTransaction
                }

                val arrayARAM = HashMap<String, PostgreTest.DataChampionWinstreak>()
                val arrayCLASSIC = HashMap<String, PostgreTest.DataChampionWinstreak>()

                val dateCurrent = LocalDate.now()
                val modifiedDate = dateCurrent.minusMonths(1).toDate().time

                val savedParticipantsMatches = R2DBC.getParticipants { Participants.tbl_participants.LOLperson_id eq KORDLOL.LOL_id }
                val arrayMatches = R2DBC.getMatches {
                    Matches.tbl_matches.matchDateStart greaterEq modifiedDate; Matches.tbl_matches.id.inList(
                    savedParticipantsMatches.map { it.match_id }); Matches.tbl_matches.surrender eq false; Matches.tbl_matches.bots eq false
                }
                val lastParticipants = R2DBC.getParticipants { Participants.tbl_participants.LOLperson_id eq KORDLOL.LOL_id; Participants.tbl_participants.match_id.inList(arrayMatches.map { it.id }) }
                lastParticipants.forEach {
                    if (arrayMatches.find { mch -> mch.id == it.match_id }?.matchMode == "ARAM") {
                        if (arrayARAM[it.championName] == null) {
                            arrayARAM[it.championName] = PostgreTest.DataChampionWinstreak(it.championId, 1, if (it.win) 1 else 0, it.kda)
                        } else {
                            val curData = arrayARAM[it.championName]!!
                            curData.championGames++
                            curData.championWins += if (it.win) 1 else 0
                            curData.championKDA += it.kda
                            arrayARAM[it.championName] = curData
                        }
                    } else if (arrayMatches.find { mch -> mch.id == it.match_id }?.matchMode == "CLASSIC") {
                        if (arrayCLASSIC[it.championName] == null) {
                            arrayCLASSIC[it.championName] = PostgreTest.DataChampionWinstreak(it.championId, 1, if (it.win) 1 else 0, it.kda)
                        } else {
                            val curData = arrayCLASSIC[it.championName]!!
                            curData.championGames++
                            curData.championWins += if (it.win) 1 else 0
                            curData.championKDA += it.kda
                            arrayCLASSIC[it.championName] = curData
                        }
                    }
                }

                val savedPartsARAM = arrayARAM.map { it.key to it.value }.sortedByDescending { it.second.championGames }.toMap().filter { it.value.championGames >= 5 }
                var textResult = if (savedPartsARAM.isNotEmpty()) "**ARAM**\n" else ""
                savedPartsARAM.forEach { (i, pairs) ->
                    textResult += "* ${if (catchHeroForName(i) == null) i else catchHeroForName(i)?.name} Games: ${pairs.championGames} WinRate: ${((pairs.championWins.toDouble() / pairs.championGames) * 100.0).to1Digits()}% KDA: ${(pairs.championKDA / pairs.championGames).to1Digits()}\n"
                    if (textResult.length > 1000) return@forEach
                }
                val savedPartsCLASSIC = arrayCLASSIC.map { it.key to it.value }.sortedByDescending { it.second.championGames }.toMap().filter { it.value.championGames >= 10 }
                textResult += if (savedPartsCLASSIC.isNotEmpty()) "\n**CLASSIC**\n" else ""

                savedPartsCLASSIC.forEach { (i, pairs) ->
                    textResult += "* ${catchHeroForName(i)?.name} Games: ${pairs.championGames} WinRate: ${((pairs.championWins.toDouble() / pairs.championGames) * 100.0).to1Digits()}% KDA: ${(pairs.championKDA / pairs.championGames).to1Digits()}\n"
                    if (textResult.length > 1900) return@forEach
                }
                this@execute.channel.createMessage(textResult)
            }
        }
    }

//    slash("getAlliesWinrate", "Просмотр Винрейта каждого игрока сервера (в боте) по отношению к себе") {
//        execute {
//            val textCommand = "[Start command] '$name' from ${author.fullName}"
//            printLog(textCommand)
//            val guilds = R2DBC.getGuild(guild)
//            guild.sendMessage(guilds.messageIdDebug, textCommand)
//
//            val KORD =
//                R2DBC.getKORDs { tbl_kords.KORD_id eq author.toStringUID(); tbl_kords.guild_id eq guilds.id }
//                    .firstOrNull()
//            if (KORD == null) {
//                respond("Вы ${author.lowDescriptor()} не зарегистрированы в боте. Обратитесь к Администратору")
//                return@execute
//            }
//            val KORDLOL =
//                R2DBC.getKORDLOLs { tbl_kordlols.KORD_id eq KORD.id; tbl_kordlols.guild_id eq guilds.id }
//                    .firstOrNull()
//            if (KORDLOL == null) {
//                respond("Вы ${author.lowDescriptor()} не привязаны к аккаунту Лиги Легенд. Обратитесь к Администратору")
//                return@execute
//            }
//
//            val arrayARAM = HashMap<Int, ArrayList<Pair<Int, Int>>>()
//            val arrayCLASSIC = HashMap<Int, ArrayList<Pair<Int, Int>>>()
//
//            val allKORDLOLS =
//                R2DBC.getKORDLOLs { tbl_kordlols.guild_id eq guilds.id; tbl_kordlols.LOL_id notEq KORDLOL.LOL_id }
//            val savedParticipantsMatches =
//                R2DBC.getParticipants { Participants.tbl_participants.LOLperson_id eq KORDLOL.LOL_id }
//            val arrayMatches =
//                R2DBC.getMatches { Matches.tbl_matches.id.inList(savedParticipantsMatches.map { it.match_id }); Matches.tbl_matches.surrender eq false; Matches.tbl_matches.bots eq false }
//            val lastParticipants = R2DBC.getParticipants {
//                Participants.tbl_participants.match_id.inList(arrayMatches.map { it.id }); Participants.tbl_participants.LOLperson_id.inList(
//                allKORDLOLS.map { it.LOL_id })
//            }
//            lastParticipants.forEach {
//                if (arrayMatches.find { mch -> mch.id == it.match_id }?.matchMode == "ARAM") {
//                    if (arrayARAM[it.LOLperson_id] == null) {
//                        arrayARAM[it.LOLperson_id] = ArrayList()
//                        arrayARAM[it.LOLperson_id]!!.add(
//                            Pair(
//                                if (it.win) 1 else 0,
//                                if (!it.win) 1 else 0
//                            )
//                        )
//                    } else {
//                        arrayARAM[it.LOLperson_id]!!.add(
//                            Pair(
//                                if (it.win) 1 else 0,
//                                if (!it.win) 1 else 0
//                            )
//                        )
//                    }
//                } else if (arrayMatches.find { mch -> mch.id == it.match_id }?.matchMode == "CLASSIC") {
//                    if (arrayCLASSIC[it.LOLperson_id] == null) {
//                        arrayCLASSIC[it.LOLperson_id] = ArrayList()
//                        arrayCLASSIC[it.LOLperson_id]!!.add(
//                            Pair(
//                                if (it.win) 1 else 0,
//                                if (!it.win) 1 else 0
//                            )
//                        )
//                    } else {
//                        arrayCLASSIC[it.LOLperson_id]!!.add(
//                            Pair(
//                                if (it.win) 1 else 0,
//                                if (!it.win) 1 else 0
//                            )
//                        )
//                    }
//                }
//            }
//
//            val arrayStatAram = ArrayList<DataStatRate>()
//            arrayARAM.forEach { (i, pairs) ->
//                var winGames = 0.0
//                pairs.forEach { if (it.first == 1) winGames++ }
//                arrayStatAram.add(
//                    DataStatRate(
//                        lol_id = i,
//                        allGames = pairs.size,
//                        winGames = winGames
//                    )
//                )
//            }
//
//            val arrayStatClassic = ArrayList<DataStatRate>()
//            arrayCLASSIC.forEach { (i, pairs) ->
//                var winGames = 0.0
//                pairs.forEach { if (it.first == 1) winGames++ }
//                arrayStatClassic.add(
//                    DataStatRate(
//                        lol_id = i,
//                        allGames = pairs.size,
//                        winGames = winGames
//                    )
//                )
//            }
//
//            var textRespond = "**ARAM**\n"
//            arrayStatAram.sortByDescending { (it.winGames / it.allGames * 100.0).to1Digits() }
//            arrayStatAram.forEach {
//                textRespond += "* __${
//                    R2DBC.getLOLs { tbl_lols.id eq it.lol_id }.firstOrNull()
//                        ?.getCorrectNameWithTag()
//                }__ ${(it.winGames / it.allGames * 100.0).to1Digits()}% Games:${it.allGames}\n"
//            }
//            textRespond += "\n**CLASSIC**\n"
//            arrayStatClassic.sortByDescending { (it.winGames / it.allGames * 100.0).to1Digits() }
//            arrayStatClassic.forEach {
//                textRespond += "* __${
//                    R2DBC.getLOLs { tbl_lols.id eq it.lol_id }.firstOrNull()
//                        ?.getCorrectNameWithTag()
//                }__ ${(it.winGames / it.allGames * 100.0).to1Digits()}% Games:${it.allGames}\n"
//            }
//            respond(textRespond)
//        }
//    }

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
            val KORD =
                R2DBC.getKORDs { tbl_kords.KORD_id eq user.toStringUID(); tbl_kords.guild_id eq guilds.id }
                    .firstOrNull()
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

    slash("initMainChannel", "Основной канал для бота", Permissions(Permission.Administrator)) {
        execute(ChannelArg<TextChannel>("channel")) {
            val (channel) = args
            val textCommand =
                "[Start command] '$name' from ${author.fullName} with params: 'channel=${channel.name}'"
            printLog(textCommand)
            val r2Guild = R2DBC.getGuild(guild)
            guild.sendMessage(r2Guild.messageIdDebug, textCommand)
            r2Guild.botChannelId = channel.id.value.toString()
            r2Guild.update()
            respond("Guild channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash(
        "clearMainChannel",
        "Очистка основного канал для бота",
        Permissions(Permission.Administrator)
    ) {
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
            val textCommand =
                "[Start command] '$name' from ${author.fullName} with params: 'channel=${channel.name}'"
            val r2Guild = R2DBC.getGuild(guild)
            guild.sendMessage(r2Guild.messageIdDebug, textCommand)
            r2Guild.messageIdStatus = channel.id.value.toString()
            r2Guild.update()
            respond("Guild status channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash(
        "clearStatusChannel",
        "Очистка канала для сообщений бота",
        Permissions(Permission.Administrator)
    ) {
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

    slash(
        "initDebugChannel",
        "Канал для системных сообщений",
        Permissions(Permission.Administrator)
    ) {
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

    slash(
        "clearDebugChannel",
        "Очистка канала для системных сообщений",
        Permissions(Permission.Administrator)
    ) {
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
            KORD = R2DBC.getKORDs { tbl_kords.KORD_id eq user.toStringUID(); tbl_kords.guild_id eq guilds.id }.firstOrNull() ?: KORD.create(KORDs::KORD_id)

            //Создаем LOL сразу со связью с аккаунтом Лиги Легенд
            var LOL = LOLs().connectLOL(region, summonerName, tagLine)
            if (LOL == null) {
                respond("Призыватель $summonerName#$tagLine не найден в Лиге Легенд")
                return@execute
            }

            //Если аккаунт есть в базе - ок, если нет - создаём в базе
            LOL = R2DBC.getLOLs { tbl_lols.LOL_puuid eq LOL!!.LOL_puuid }.firstOrNull() ?: LOL.create(LOLs::LOL_puuid)

            //Проверка что к пользователю уже привязан какой-либо аккаунт лиги
            val alreadyKORDLOL =
                R2DBC.getKORDLOLs { tbl_kordlols.KORD_id eq KORD.id; tbl_kordlols.guild_id eq guilds.id }
                    .firstOrNull()
            if (alreadyKORDLOL != null) {
                respond("Призыватель $summonerName#$tagLine уже связан с аккаунтом лиги легенд (KORDLOL: ${alreadyKORDLOL.id}). Для внесения изменений - сначала удалите из базы пользователя ${user.lowDescriptor()}")
                return@execute
            }

            asyncLaunch {
                val KORDLOL = KORDLOLs(
                    KORD_id = KORD.id,
                    LOL_id = LOL.id,
                    guild_id = guilds.id
                )

                val resultData = KORDLOL.create(KORDLOLs::KORD_id)
                val arrayData = ArrayList<KORDLOLs>()
                arrayData.addAll(R2DBC.getKORDLOLs { tbl_kordlols.guild_id eq guilds.id })
                arrayData.sortBy { data -> data.showCode }
                resultData.showCode = arrayData.last().showCode + 1
                resultData.update()
            }
            mapMainData[guild]!!.isNeedUpdateDatas = true
            mapMainData[guild]!!.isNeedUpdateDays = true
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

    slash("addHeroName", "Добавить наименование чемпиона", Permissions(Permission.UseApplicationCommands)) {
        execute(AutocompleteArg("hero", description = "Чемпион Лиги Легенд", type = AnyArg(), autocomplete = {R2DBC.getHeroes().map { it.nameRU }}), AnyArg("newNames", description = "Новое имя чемпиона (несколько через ',')")) {
            val (hero, newNames) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'hero'=$hero, 'newNames'=$newNames"
            printLog(textCommand)

            val findObj = R2DBC.getHeroesone({tbl_heroes.nameRU eq hero})
            val retText = if (findObj == null) {
                "Чемпион $hero не найден. Обратитесь к Администратору"
            } else {
                val curNames = findObj.otherNames.split(",")
                val arrayNewNames = ArrayList<String>()
                arrayNewNames.addAll(curNames)
                var appendText = ""
                newNames.split(",").forEach { str ->
                    val cStr = str.lowercase().trim().trim(',')
                    if (!arrayNewNames.contains(cStr)) {
                        val fObj = R2DBC.getHeroes().find { f -> f.otherNames.contains(cStr) }
                        if (fObj == null) {
                            arrayNewNames.add(cStr)
                        } else {
                            appendText += "\nимя $cStr не было сохранено, т.к. содержится у Героя ${fObj.nameRU}\n"
                        }
                    }
                }
                findObj.otherNames = arrayNewNames.joinToString(separator = ",")
                findObj.update()
                "Успешно. $appendText Текущий список имен (${findObj.nameRU}): ${arrayNewNames.joinToString(separator = ",")}"
            }

            respond(retText)
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

    slash("userDelete", "Удалить учётную запись из базы данных бота", Permissions(Permission.Administrator)) {
        execute(UserArg("User", "Пользователь Discord")) {
            val (user) = args
            val textCommand =
                "[Start command] '$name' from ${author.fullName} with params: 'user=${user.fullName}'"
            printLog(textCommand)

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            val dataKORD =
                R2DBC.getKORDs { tbl_kords.KORD_id eq user.toStringUID(); tbl_kords.guild_id eq guilds.id }
                    .firstOrNull()
            val textMessage = if (dataKORD == null) {
                "Пользователя не существует в базе"
            } else {
                dataKORD.deleteWithKORDLOL(guilds)
                "Удаление произошло успешно"
            }
            mapMainData[guild]!!.isNeedUpdateDays = true
            respond(textMessage)
        }
    }

    slash("userDeleteFromID", "Удалить учётную запись из базы данных бота по show ID", Permissions(Permission.Administrator)) {
        execute(IntegerArg("id", "show id пользователя Discord")) {
            val (id) = args
            val textCommand = "[Start command] '$name' from ${author.fullName} with params: 'id=$id'"
            printLog(textCommand)

            val guilds = R2DBC.getGuild(guild)
            guild.sendMessage(guilds.messageIdDebug, textCommand)

            val dataKORD = R2DBC.getKORDLOLs { tbl_kordlols.showCode eq id; tbl_kordlols.guild_id eq guilds.id }

            if (dataKORD.isEmpty()) {
                respond("Пользователя с id $id в базе не найдено. Операция отменена. Обратитесь к Администратору")
                return@execute
            }

            if (dataKORD.size > 1) {
                respond("Пользователей с id $id больше 1: ${dataKORD.size}. Операция отменена. Обратитесь к Администратору")
                return@execute
            }

            val textMessage = run {
                val kordId = dataKORD.first().KORD_id
                dataKORD.first().delete()
                R2DBC.getKORDs { tbl_kords.id eq kordId }.firstOrNull()?.delete()
                "Удаление произошло успешно"
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
                dataUser.mmrAramSaved += savedMMR.toDouble().to1Digits()
                dataUser.update()
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
                dataUser.mmrAramSaved -= savedMMR.toDouble().to1Digits()
                dataUser.update()
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

            val dataUser = R2DBC.getKORDs { tbl_kords.KORD_id eq user.toStringUID() }.firstOrNull()
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