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
import ru.descend.bot.firebase.CompleteResult
import ru.descend.bot.firebase.F_PENTAKILLS
import ru.descend.bot.firebase.F_PENTASTILLS
import ru.descend.bot.firebase.FireChampion
import ru.descend.bot.firebase.FireKordPerson
import ru.descend.bot.firebase.FirePKill
import ru.descend.bot.firebase.FirePSteal
import ru.descend.bot.firebase.FirePerson
import ru.descend.bot.firebase.FirebaseService
import ru.descend.bot.isBot
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
        execute(UserArg("User", "Пользователь Discord"), ChoiceArg<String>("Region", "Регион аккаунта Лиги легенд", "ru"), AnyArg("SummonerName", "Имя призывателя в Лиге легенд")){
            val (user, region, summonerName) = args

            printLog("Start command '$name' from ${author.fullName} with params: 'user=${user.fullName}', 'region=$region', 'summonerName=$summonerName'")
            var newUser = FirebaseService.getUser(guild, user.toStringUID())
            if (newUser == null) {
                val person = FirePerson()
                person.initKORD(user)
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

            respond(textMessage)
        }
    }

    slash("pkill", "Запишите того, кто сделал Пентакилл", Permissions(Permission.Administrator)){
        execute(UserArg("Who", "Кто сделал Пентакилл"), AutocompleteArg("hero", "За какого героя был сделан Пентакилл",
            type = AnyArg, autocomplete = {
                LeagueMainObject.heroObjects.filter { (it as InterfaceChampionBase).name.lowercase().contains(this.input.lowercase()) }.map { (it as InterfaceChampionBase).name }
            })) {
            val (userWho, hero) = args

            printLog("Start command '$name' from ${author.fullName} with params: 'userWho=${userWho.fullName}', 'hero=$hero'")

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

            val findObjHero = LeagueMainObject.findHeroForName(hero)

            val person = FirePerson()
            person.initKORD(userWho)
            FirebaseService.addPerson(guild, person)
            FirebaseService.addPentaKill(guild, person, FirePKill(FireChampion.catchFromDTO(findObjHero)))
            val newUser = FirebaseService.getUser(guild, person.KORD_id)
            val pentaData = FirebaseService.getArrayFromCollection<FirePKill>(newUser!!.toDocument().collection(F_PENTAKILLS))

            respondPublic {
                title = "ПЕНТАКИЛЛ"
                description = "Призыватель ${userWho.lowDescriptor()} сделал внезапную Пенту за чемпиона '$hero'. Поздравляем!"
                footer("Всего пентакиллов: ${pentaData.size}")
            }
        }
    }

    slash("pstill", "Запишите того, кто сделал Пентастилл", Permissions(Permission.Administrator)){
        execute(
            AutocompleteArg("hero", "За какого героя был сделан Пентастилл", type = AnyArg, autocomplete = {
                LeagueMainObject.heroObjects.filter { (it as InterfaceChampionBase).name.lowercase().contains(this.input.lowercase()) }.map { (it as InterfaceChampionBase).name }
            }),
            UserArg("Who", "Кто сделал Пентастилл (оставь пустым если Ноунейм)").optional{ Configuration.getBotAsUser(kord = discord.kord) },
            UserArg("FromWhom", "У кого был состилен Пентакилл (оставь пустым если у Ноунейма)").optional{ Configuration.getBotAsUser(kord = discord.kord) }){
            val (heroSteal, userWho, userFromWhom) = args

            printLog("Start command '$name' from ${author.fullName} with params: 'heroSteal=$heroSteal', 'userWho=${userWho.fullName}', 'userFromWhom=${userFromWhom.fullName}'")

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

            val findObjHero = LeagueMainObject.findHeroForName(heroSteal)

            val personWho = FirePerson()
            val personFromWhom = FirePerson()
            personWho.initKORD(userWho)
            personFromWhom.initKORD(userFromWhom)

            if (userWho.isBot()) {
                FirebaseService.addPerson(guild, personFromWhom)
            }
            else if (userFromWhom.isBot()) {
                FirebaseService.addPerson(guild, personWho)
            }
            else {
                FirebaseService.addPerson(guild, personWho)
                FirebaseService.addPerson(guild, personFromWhom)
            }

            val psTeal = FirePSteal()
            psTeal.hero = FireChampion.catchFromDTO(findObjHero)

            val description = if (userWho.isBot()) {
                psTeal.whoSteal = null
                psTeal.fromWhomSteal = FireKordPerson.initKORD(userFromWhom)
                "Какой-то ноунейм за чемпиона '$heroSteal' состилил пенту у высокоуважаемого ${userFromWhom.lowDescriptor()}"
            } else if (userFromWhom.isBot()) {
                psTeal.whoSteal = FireKordPerson.initKORD(userWho)
                psTeal.fromWhomSteal = null
                "Красавчик ${userWho.lowDescriptor()} за чемпиона '$heroSteal' состилил пенту у Щегола какого-то"
            } else {
                psTeal.whoSteal = FireKordPerson.initKORD(userWho)
                psTeal.fromWhomSteal = FireKordPerson.initKORD(userFromWhom)
                "Соболезнуем, но ${userWho.lowDescriptor()} случайно состилил пенту за чемпиона '$heroSteal' у ${userFromWhom.lowDescriptor()}"
            }

            if (!userWho.isBot() && !userFromWhom.isBot()){
                FirebaseService.addPentaSteal(guild, personWho, psTeal)
                FirebaseService.addPentaSteal(guild, personFromWhom, psTeal)
            } else {
                FirebaseService.addPentaSteal(guild, if (userWho.isBot()) personFromWhom else personWho, psTeal)
            }

            val iUser = if (userWho.isBot()) personFromWhom else personWho
            val newUser = FirebaseService.getUser(guild, iUser.KORD_id)
            val stealData = FirebaseService.getArrayFromCollection<FirePSteal>(newUser!!.toDocument().collection(F_PENTASTILLS))

            val textPStillWho = stealData.filter { it.whoSteal != null }.filter { it.whoSteal!!.snowflake == newUser.KORD_id }.size
            val textPStillWhom = stealData.filter { it.fromWhomSteal != null }.filter { it.fromWhomSteal!!.snowflake == newUser.KORD_id }.size

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