package ru.descend.bot.commands

import delete
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.TextChannel
import me.jakejmattson.discordkt.arguments.*
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.extensions.fullName
import ru.descend.bot.asyncLaunch
import ru.descend.bot.globalLOLRequests
import ru.descend.bot.isWorkMainThread
import ru.descend.bot.launch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lowDescriptor
import ru.descend.bot.postgre.TableKORDPerson
import ru.descend.bot.postgre.TableKORD_LOL
import ru.descend.bot.postgre.TableLOLPerson
import ru.descend.bot.postgre.PostgreSQL
import ru.descend.bot.postgre.PostgreSQL.getGuild
import ru.descend.bot.postgre.TableGuild
import ru.descend.bot.printLog
import ru.descend.bot.reloadMatch
import ru.descend.bot.sendMessage
import ru.descend.bot.showLeagueHistory
import ru.descend.bot.sqlCurrentUsers
import save
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

    slash("resetHistory", "Перезагрузить историю сервера", Permissions(Permission.Administrator)){
        execute {
            printLog("Start command '$name' from ${author.fullName}")
            asyncLaunch {
                launch {
                    showLeagueHistory(guild, getGuild(guild))
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

            val KORD = TableKORDPerson(guild, user).save()
            val LOL = TableLOLPerson(region, summonerName).save()
            val KORDLOL = TableKORD_LOL(KORDperson = KORD, LOLperson = LOL).save()

            sqlCurrentUsers[guild]!!.add(KORDLOL!!)
            printLog(guild, "Array Users ++. Size: ${sqlCurrentUsers[guild]!!.size}")

            val curGuild = getGuild(guild)

            asyncLaunch {
                guild.sendMessage(curGuild.messageIdDebug, "Запущен процесс прогрузки матчей для пользователя ${KORDLOL.asUser(guild).lowDescriptor()}")
                LeagueMainObject.catchMatchID(LOL!!.LOL_puuid, 0,50).forEach { matchId ->
                    LeagueMainObject.catchMatch(matchId)?.let { match ->
                        curGuild.addMatch(guild, match)
                    }
                }
            }.invokeOnCompletion {
                launch {
                    guild.sendMessage(curGuild.messageIdDebug, "Матчи успешно загружены для пользователя ${KORDLOL.asUser(guild).lowDescriptor()}")
                }
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
                        reloadMatch(guild, it.LOLperson!!.LOL_puuid, startIndex)
                    }
                    guild.sendMessage(getGuild(guild).messageIdDebug, "Прогрузка матчей (с $startIndex в количестве 100) для пользователя ${user.lowDescriptor()} завершена")
                }
                "Прогрузка успешно запущена"
            }

            respond(textMessage)
        }
    }
}