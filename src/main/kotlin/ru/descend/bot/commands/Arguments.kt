package ru.descend.bot.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import me.jakejmattson.discordkt.arguments.*
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.extensions.fullName
import ru.descend.bot.MAIN_ROLE_NAME
import ru.descend.bot.SECOND_ROLE_NAME
import ru.descend.bot.arrayCurrentMatches
import ru.descend.bot.arrayCurrentUsers
import ru.descend.bot.checkPermission
import ru.descend.bot.checkRoleForName
import ru.descend.bot.firebase.CompleteResult
import ru.descend.bot.firebase.F_USERS
import ru.descend.bot.firebase.FireMatch
import ru.descend.bot.firebase.FirePerson
import ru.descend.bot.firebase.FirebaseService
import ru.descend.bot.isBotOwner
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lowDescriptor
import ru.descend.bot.printLog
import ru.descend.bot.toStringUID

private suspend fun checkCommandsAccess(guild: Guild, author: User) : Boolean {
    if (!author.checkRoleForName(guild, SECOND_ROLE_NAME) && !author.checkRoleForName(guild, MAIN_ROLE_NAME) && !author.checkPermission(guild, Permission.Administrator) && !author.isBotOwner()){
        return false
    }
    return true
}

fun arguments() = commands("Arguments") {

    slash("initializeBotChannel", "Основной канал для бота", Permissions(Permission.Administrator)){
        execute(ChannelArg<TextChannel>("channel")){
            val (channel) = args

            printLog("Start command '$name' from ${author.fullName} with params: 'channel=${channel.name}'")

            var curGuild = FirebaseService.getGuild(guild)
            if (curGuild == null){
                FirebaseService.addGuild(guild)
                curGuild = FirebaseService.getGuild(guild)
            }
            curGuild!!.botChannelId = channel.id.value.toString()
            curGuild.fireSaveData()

            respond("Guild channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash("pConnectUser", "Связать учетную запись Лиги легенд и пользователя Discord", Permissions(Permission.Administrator)){
        execute(UserArg("User", "Пользователь Discord"), ChoiceArg("Region", "Регион аккаунта Лиги легенд", "ru", "br1", "eun1", "euw1", "jp1", "kr", "la1", "la2", "na1", "oc1", "tr1", "pbe1"), AnyArg("SummonerName", "Имя призывателя в Лиге легенд")){
            val (user, region, summonerName) = args

            printLog("Start command '$name' from ${author.fullName} with params: 'user=${user.fullName}', 'region=$region', 'summonerName=$summonerName'")
            var newUser = FirebaseService.getUser(guild, user.toStringUID())
            if (newUser == null) {
                val person = FirePerson()
                person.initKORD(user)
                person.personIndex = FirebaseService.getArrayFromCollection<FirePerson>(FirebaseService.collectionGuild(guild, F_USERS)).await().size
                when (val res = FirebaseService.addPerson(guild, person)){
                    is CompleteResult.Error -> printLog(res.errorText)
                    is CompleteResult.Success -> null
                }
                newUser = FirebaseService.getUser(guild, person.KORD_id)
            }
            val res = newUser!!.initLOL(region, summonerName)
            var textMessage = ""
            textMessage = when (res) {
                is CompleteResult.Error -> {
                    res.errorText
                }
                is CompleteResult.Success -> {
                    newUser.fireSaveData()
                    "Пользователь ${user.lowDescriptor()} связан с учётной записью $region $summonerName"
                }
            }

            arrayCurrentUsers[guild.id.value.toString()]!!.add(newUser)
            printLog(guild, "Array Users ++. Size: ${arrayCurrentUsers[guild.id.value.toString()]!!.size}")

            var newMatches = 0
            LeagueMainObject.catchMatchID(newUser.LOL_puuid, 50).forEach { matchId ->
                LeagueMainObject.catchMatch(matchId)?.let { match ->
                    when (FirebaseService.addMatchToGuild(guild, match)) {
                        is CompleteResult.Error -> null
                        is CompleteResult.Success -> {
                            newMatches++
                        }
                    }
                }
            }

            textMessage += "\nДобавлено $newMatches матчей игрока"
            respond(textMessage)
        }
    }

    slash("pDeleteUser", "Удалить учётную запись из базы данных бота", Permissions(Permission.Administrator)){
        execute(UserArg("User", "Пользователь Discord")){
            val (user) = args
            printLog("Start command '$name' from ${author.fullName} with params: 'user=${user.fullName}'")
            val newUser = FirebaseService.getUser(guild, user.toStringUID())
            val textMessage = if (newUser == null) {
                "Пользователя не существует в базе"
            } else {
                when (val data = newUser.deleteData()) {
                    is CompleteResult.Error -> {
                        data.errorText
                    }

                    is CompleteResult.Success -> {
                        "Пользователь ${user.lowDescriptor()} успешно удалён из базы"
                    }
                }
            }
            respond(textMessage)
        }
    }
}