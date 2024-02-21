package ru.descend.bot.commands

import delete
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.entity.channel.TextChannel
import me.jakejmattson.discordkt.arguments.*
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.extensions.fullName
import ru.descend.bot.asyncLaunch
import ru.descend.bot.isWorkMainThread
import ru.descend.bot.launch
import ru.descend.bot.lowDescriptor
import ru.descend.bot.mainMapData
import ru.descend.bot.postgre.execProcedure
import ru.descend.bot.postgre.getGuild
import ru.descend.bot.postgre.tables.TableKORDPerson
import ru.descend.bot.postgre.tables.TableKORD_LOL
import ru.descend.bot.postgre.tables.TableLOLPerson
import ru.descend.bot.postgre.tables.TableGuild
import ru.descend.bot.postgre.tables.tableKORDLOL
import ru.descend.bot.postgre.tables.tableKORDPerson
import ru.descend.bot.postgre.tables.tableLOLPerson
import ru.descend.bot.printLog
import ru.descend.bot.reloadMatch
import ru.descend.bot.sendMessage
import ru.descend.bot.showLeagueHistory
import ru.descend.bot.to2Digits
import save
import statements.selectAll
import update

fun arguments() = commands("Arguments") {

    slash("initMainChannel", "Основной канал для бота", Permissions(Permission.Administrator)){
        execute(ChannelArg<TextChannel>("channel")){
            val (channel) = args
            printLog("Start command '$name' from ${author.fullName} with params: 'channel=${channel.name}'")
            getGuild(guild).update(TableGuild::botChannelId) {
                this.botChannelId = channel.id.value.toString()
            }
            respond("Guild channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash("clearMainChannel", "Очистка основного канал для бота", Permissions(Permission.Administrator)){
        execute {
            printLog("Start command '$name' from ${author.fullName}")
            getGuild(guild).update(TableGuild::botChannelId) {
                this.botChannelId = ""
            }
            respond("Guild MainChannel cleared")
        }
    }

    slash("resetMMRtable", "Перезагрузка таблицы средних рейтингов", Permissions(Permission.Administrator)){
        execute {
            printLog("Start command '$name' from ${author.fullName}")
            execProcedure("call \"GetAVGs\"()")
            respond("Успешно")
        }
    }

    slash("initStatusChannel", "Канал для сообщений бота", Permissions(Permission.Administrator)){
        execute(ChannelArg<TextChannel>("channel")){
            val (channel) = args
            printLog("Start command '$name' from ${author.fullName} with params: 'channel=${channel.name}'")
            getGuild(guild).update(TableGuild::messageIdStatus) {
                this.messageIdStatus = channel.id.value.toString()
            }
            respond("Guild status channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash("clearStatusChannel", "Очистка канала для сообщений бота", Permissions(Permission.Administrator)){
        execute {
            printLog("Start command '$name' from ${author.fullName}")
            getGuild(guild).update(TableGuild::messageIdStatus) {
                this.messageIdStatus = ""
            }
            respond("Guild StatusChannel cleared")
        }
    }

    slash("initDebugChannel", "Канал для системных сообщений", Permissions(Permission.Administrator)){
        execute(ChannelArg<TextChannel>("channel")){
            val (channel) = args
            printLog("Start command '$name' from ${author.fullName} with params: 'channel=${channel.name}'")
            getGuild(guild).update(TableGuild::messageIdDebug) {
                this.messageIdDebug = channel.id.value.toString()
            }
            respond("Guild status channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash("clearDebugChannel", "Очистка канала для системных сообщений", Permissions(Permission.Administrator)){
        execute {
            printLog("Start command '$name' from ${author.fullName}")
            getGuild(guild).update(TableGuild::messageIdDebug) {
                this.messageIdDebug = ""
            }
            respond("Guild DebugChannel cleared")
        }
    }

    slash("resetHistory", "Перезагрузить историю сервера", Permissions(Permission.Administrator)){
        execute {
            printLog("Start command '$name' from ${author.fullName}")
            asyncLaunch {
                launch {
                    showLeagueHistory(guild)
                    isWorkMainThread[guild] = false
                }.invokeOnCompletion {
                    launch {
                        guild.sendMessage(getGuild(guild).messageIdDebug, "Перезагрузка истории сервера прошла завершена")
                        isWorkMainThread[guild] = true
                        printLog(guild, "[Arguments] isWorkMainThread true")
                    }
                }
            }
            respond("Перезагрузка сервера успешно запущена")
        }
    }

    slash("userCreate", "Создание учетной записи Лиги легенд и пользователя Discord", Permissions(Permission.Administrator)){
        execute(UserArg("User", "Пользователь Discord"), ChoiceArg("Region", "Регион аккаунта Лиги легенд", "ru", "br1", "eun1", "euw1", "jp1", "kr", "la1", "la2", "na1", "oc1", "tr1", "pbe1"), AnyArg("SummonerName", "Имя призывателя в Лиге легенд")){
            val (user, region, summonerName) = args

            printLog("Start command '$name' from ${author.fullName} with params: 'user=${user.fullName}', 'region=$region', 'summonerName=$summonerName'")

            val curGuild = getGuild(guild)

            var KORD = TableKORDPerson(guild, user)
            var findKORD = mainMapData[guild]?.getKORD()?.find { it.KORD_id == KORD.KORD_id }
            if (findKORD == null) {
                findKORD = tableKORDPerson.selectAll().where { TableKORDPerson::guild eq curGuild }.where { TableKORDPerson::KORD_id eq user.id.value.toString() }.getEntity()
            }
            if (findKORD == null){
                KORD.save()
            } else {
                KORD = findKORD
            }

            var LOL = TableLOLPerson(region, summonerName)
            var findLOL = mainMapData[guild]?.getLOL()?.find { it.LOL_puuid == LOL.LOL_puuid }
            if (findLOL == null) {
                findLOL = tableLOLPerson.selectAll().where { TableLOLPerson::LOL_puuid eq LOL.LOL_puuid }.getEntity()
            }
            if (findLOL == null){
                LOL.save()
            } else {
                LOL = findLOL
            }

            val KORDLOL = TableKORD_LOL(
                KORDperson = KORD,
                LOLperson = LOL,
                guild = curGuild)
            val findKORDLOL = tableKORDLOL.selectAll().where { TableKORDPerson::KORD_id eq KORD.KORD_id }.where { TableLOLPerson::LOL_puuid eq LOL.LOL_puuid }.getEntity()
            if (findKORDLOL == null){
                KORDLOL.save()
            } else {
                respond("Указанный пользователь(${KORD.id}) уже связан с указанным аккаунтом лиги легенд(${LOL.id})")
                return@execute
            }

            respond("Запущен процесс добавления пользователя и игрока в базу")
        }
    }

    slash("userDelete", "Удалить учётную запись из базы данных бота", Permissions(Permission.Administrator)){
        execute(UserArg("User", "Пользователь Discord")){
            val (user) = args
            printLog("Start command '$name' from ${author.fullName} with params: 'user=${user.fullName}'")
            val dataUser = TableKORD_LOL.getForKORD(user)
            val textMessage = if (dataUser == null) {
                "Пользователя не существует в базе"
            } else {
                TableKORD_LOL.deleteForKORD(dataUser.first.id)
                dataUser.second.forEach {
                    TableKORD_LOL.deleteForLOL(it.LOLperson?.id?:-1)
                    it.delete()
                }
                asyncLaunch {
                    guild.sendMessage(getGuild(guild).messageIdDebug, "Удаление пользователя ${user.lowDescriptor()} завершено")
                }
                "Удаление произошло успешно"
            }
            respond(textMessage)
        }
    }

    slash("addSavedMMR", "Добавить бонусные MMR пользователю", Permissions(Permission.Administrator)){
        execute(UserArg("user", "Пользователь Discord"), IntegerArg("savedMMR", "Количество бонусных MMR")){
            val (user, savedMMR) = args
            printLog("Start command '$name' from ${author.fullName} with params: 'user=$user' 'savedMMR=$savedMMR'")
            val dataUser = TableKORD_LOL.getForKORD(user)
            val textMessage = if (dataUser == null){
                "Пользователя не существует в базе"
            } else {
                asyncLaunch {
                    dataUser.second.forEach {
                        it.update(TableKORD_LOL::mmrAramSaved){
                            mmrAramSaved += savedMMR.toDouble().to2Digits()
                        }
                    }
                }
                "Сохранение успешно произведено"
            }
            respond(textMessage)
        }
    }

    slash("removeSavedMMR", "Вычесть бонусные MMR пользователю", Permissions(Permission.Administrator)){
        execute(UserArg("user", "Пользователь Discord"), IntegerArg("savedMMR", "Количество вычитаемых MMR")){
            val (user, savedMMR) = args
            printLog("Start command '$name' from ${author.fullName} with params: 'user=$user' 'savedMMR=$savedMMR'")
            val dataUser = TableKORD_LOL.getForKORD(user)
            val textMessage = if (dataUser == null){
                "Пользователя не существует в базе"
            } else {
                asyncLaunch {
                    dataUser.second.forEach {
                        it.update(TableKORD_LOL::mmrAramSaved){
                            val currentMMR = mmrAramSaved
                            if (currentMMR - savedMMR.toDouble().to2Digits() > 0){
                                mmrAramSaved -= savedMMR.toDouble().to2Digits()
                            } else {
                                mmrAramSaved = 0.0
                            }
                        }
                    }
                }
                "Сохранение успешно произведено"
            }
            respond(textMessage)
        }
    }

    slash("userDeleteFromId", "Удалить учётную запись из базы данных бота по ID пользователя", Permissions(Permission.Administrator)){
        execute(IntegerArg("UserId", "Пользователь Discord ID в базе")){
            val (UserId) = args
            printLog("Start command '$name' from ${author.fullName} with params: 'user=$UserId'")
            val dataUserKORD = tableKORDPerson[UserId]
            val textMessage = if (dataUserKORD == null) {
                "Пользователя с id $UserId не существует в базе"
            } else {
                TableKORD_LOL.deleteForKORD(dataUserKORD.id)
                asyncLaunch {
                    guild.sendMessage(getGuild(guild).messageIdDebug, "Удаление пользователя $UserId завершено")
                }
                "Удаление произошло успешно"
            }
            respond(textMessage)
        }
    }

    slash("pLoadMatches", "Прогрузить очередные 100 игр пользователя", Permissions(Permission.Administrator)){
        execute(UserArg("User", "Пользователь Discord"), IntegerArg("startIndex", "Начиная с какой игры (с последней) прогружать матчи")){
            val (user, startIndex) = args
            printLog("Start command '$name' from ${author.fullName} with params: 'user=${user.fullName}' 'startIndex=$startIndex'")

            val dataUser = TableKORD_LOL.getForKORD(user)
            val textMessage = if (dataUser == null){
                "Пользователя не существует в базе"
            } else {
                asyncLaunch {
                    dataUser.second.forEach {
                        if (it.LOLperson == null) return@forEach
                        reloadMatch(mainMapData[guild]!!, it.LOLperson!!.LOL_puuid, startIndex)
                    }
                    guild.sendMessage(getGuild(guild).messageIdDebug, "Прогрузка матчей (с $startIndex в количестве 100) для пользователя ${user.lowDescriptor()} завершена")
                }
                "Прогрузка успешно запущена"
            }

            respond(textMessage)
        }
    }
}