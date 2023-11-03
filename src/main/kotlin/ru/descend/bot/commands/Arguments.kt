package ru.descend.bot.commands

import ru.descend.bot.lolapi.champions.InterfaceChampionBase
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import me.jakejmattson.discordkt.arguments.*
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.extensions.footer
import me.jakejmattson.discordkt.extensions.fullName
import ru.descend.bot.MAIN_ROLE_NAME
import ru.descend.bot.SECOND_ROLE_NAME
import ru.descend.bot.checkPermission
import ru.descend.bot.checkRoleForName
import ru.descend.bot.data.Configuration
import ru.descend.bot.isBot
import ru.descend.bot.isBotOwner
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lowDescriptor
import ru.descend.bot.savedObj.Person
import ru.descend.bot.savedObj.readDataFile
import ru.descend.bot.savedObj.readPersonFile
import ru.descend.bot.savedObj.writeDataFile
import ru.descend.bot.savedObj.writePersonFile
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

            println("Start command '$name' from ${author.fullName} with params: 'channel=${channel.name}'")

            val data = readDataFile(guild)
            data.botChannelId = channel.id.value.toString()
            writeDataFile(guild, data)

            respond("Guild channel saved in '${channel.name}(${channel.id.value})'")
        }
    }

    slash("pkill", "Запишите того, кто сделал Пентакилл"){
        execute(UserArg("Who"), AutocompleteArg("hero", "За какого героя была сделана Пента",
            type = AnyArg, autocomplete = {
                LeagueMainObject.heroObjects.filter { (it as InterfaceChampionBase).name.lowercase().contains(this.input.lowercase()) }.map { (it as InterfaceChampionBase).name }
            })) {
            val (userWho, hero) = args

            println("Start command '$name' from ${author.fullName} with params: 'userWho=${userWho.fullName}', 'hero=$hero'")

            if (!checkCommandsAccess(guild, author)){
                respond("У вас нет доступа к данной команде. Обратитесь к Администратору")
                return@execute
            }

            if (userWho.isBot()){
                respond("Какого хрена? Бот красавчик в отличии от тебя")
                return@execute
            }

            if (!LeagueMainObject.heroNames.contains(hero)){
                respond("Выбран чет какой-то кривой герой, попробуй еще раз")
                return@execute
            }

            val findObjHero = LeagueMainObject.heroObjects.find { (it as InterfaceChampionBase).name == hero } as InterfaceChampionBase
            val data = readPersonFile(guild)
            data.addPersons(Person(userWho))
            data.addPentaKill(userWho.id.value.toString(), findObjHero.key)
            writePersonFile(guild, data)

            respondPublic {
                title = "ПЕНТАКИЛЛ"
                description = "Призыватель ${userWho.lowDescriptor()} сделал внезапную Пенту за чемпиона '$hero'. Поздравляем!"
                footer("Всего пентакиллов: ${data.findForUUID(userWho.id.value.toString())!!.pentaKills.size}")
            }
        }
    }

    slash("pstill", "Запишите того, кто сделал Пентастилл"){
        execute(
            AutocompleteArg("hero", "За какого героя была сделана Пента", type = AnyArg, autocomplete = {
                LeagueMainObject.heroObjects.filter { (it as InterfaceChampionBase).name.lowercase().contains(this.input.lowercase()) }.map { (it as InterfaceChampionBase).name }
            }),
            UserArg("Who").optional{ Configuration.getBotAsUser(kord = discord.kord) },
            UserArg("FromWhom").optional{ Configuration.getBotAsUser(kord = discord.kord) }){
            val (heroSteal, userWho, userFromWhom) = args

            println("Start command '$name' from ${author.fullName} with params: 'heroSteal=$heroSteal', 'userWho=${userWho.fullName}', 'userFromWhom=${userFromWhom.fullName}'")

            if (!checkCommandsAccess(guild, author)){
                respond("У вас нет доступа к данной команде. Обратитесь к Администратору")
                return@execute
            }

            if (userWho.isBot() && userFromWhom.isBot()){
                respond("ОШИБКА: Хотя бы один параметр должен быть указан")
                return@execute
            }

            if (userWho.id == userFromWhom.id){
                respond("Сам у себя стилишь... Извращенец. Так нельзя")
                return@execute
            }

            if (heroSteal.isBlank()){
                respond("Должен быть выбран герой, состилящий пенту")
                return@execute
            }

            val findObjHero = LeagueMainObject.heroObjects.find { (it as InterfaceChampionBase).name == heroSteal } as InterfaceChampionBase
            val data = readPersonFile(guild)
            if (userWho.isBot()) data.addPersons(Person(userFromWhom))
            else if (userFromWhom.isBot()) data.addPersons(Person(userWho))
            else {
                data.addPersons(Person(userWho))
                data.addPersons(Person(userFromWhom))
            }

            if (!userWho.isBot() && !userFromWhom.isBot()){
                data.addPentaStill(userWho, userWho.toStringUID(), userFromWhom.toStringUID(), findObjHero.key)
                data.addPentaStill(userFromWhom, userWho.toStringUID(), userFromWhom.toStringUID(), findObjHero.key)
            } else {
                data.addPentaStill(if (userWho.isBot()) userFromWhom else userWho, if (userWho.isBot()) "0" else userWho.toStringUID(), if (userFromWhom.isBot()) "0" else userFromWhom.toStringUID(), findObjHero.key)
            }

            writePersonFile(guild, data)

            val description = if (userWho.isBot()) {
                "Какой-то ноунейм за чемпиона '$heroSteal' состилил пенту у высокоуважаемого ${userFromWhom.lowDescriptor()}"
            } else if (userFromWhom.isBot()) {
                "Красавчик ${userWho.lowDescriptor()} за чемпиона '$heroSteal' состилил пенту у Щегола какого-то"
            } else {
                "Соболезнуем, но ${userWho.lowDescriptor()} случайно состилил пенту за чемпиона '$heroSteal' у ${userFromWhom.lowDescriptor()}"
            }

            val iUser = if (userWho.isBot()) userFromWhom else userWho
            val textPStillWho = data.findForUUID(iUser.toStringUID())!!.pentaStills.filter { it.whoSteal == iUser.toStringUID() }.size
            val textPStillWhom = data.findForUUID(iUser.toStringUID())!!.pentaStills.filter { it.fromWhomSteal == iUser.toStringUID() }.size

            respondPublic {
                title = "ПЕНТАСТИЛЛ"
                this.description = description
                footer {
                    text = "Всего состилил: $textPStillWho\n" +
                            "Всего состилено: $textPStillWhom"
                }
            }
        }
    }
}