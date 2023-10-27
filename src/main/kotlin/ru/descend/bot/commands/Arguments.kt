package ru.descend.bot.commands

import com.google.gson.GsonBuilder
import dev.kord.common.entity.Permission
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import me.jakejmattson.discordkt.arguments.*
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.extensions.descriptor
import ru.descend.bot.MAIN_ROLE_NAME
import ru.descend.bot.checkPermission
import ru.descend.bot.checkRoleForName
import ru.descend.bot.data.Configuration
import ru.descend.bot.isBotOwner
import ru.descend.bot.savedObj.Person


private suspend fun checkCommandsAccess(guild: Guild, author: User) : Boolean {
    if (!author.checkRoleForName(guild, MAIN_ROLE_NAME) && !author.checkPermission(guild, Permission.Administrator) && !author.isBotOwner()){
        return false
    }
    return true
}

//Most of the time, you will want your commands to accept input.
//This can be accomplished with the different ArgumentTypes.
fun arguments() = commands("Arguments") {

    slash("pkill", "Add a user who still Pentakill :D"){
        execute(UserArg("Who")){
            val (userWho) = args

            if (!checkCommandsAccess(guild, author)){
                respond("У вас нет доступа к данной команде. Обратитесь к Администратору")
                return@execute
            }

            if (userWho.isBot){
                respond("Какого хрена? Бот красавчик в отличии от тебя")
                return@execute
            }

//            val builder = GsonBuilder()
//            val gson = builder.create()
//            val obj = Person.create(userWho)
//            println(gson.toJson(obj))

            respond("Okay")
        }
    }

    slash("pstill", "Add a user who still Pentakill :D"){
        execute(UserArg("Who"), UserArg("FromWhom").optional{ Configuration.getBotAsUser(kord = discord.kord) } ){
            val (userWho, userFromWhom) = args

            if (!checkCommandsAccess(guild, author)){
                respond("У вас нет доступа к данной команде. Обратитесь к Администратору")
                return@execute
            }

            if (userWho.isBot){
                respond("Какого хрена? Бот красавчик в отличии от тебя")
                return@execute
            }

            respond("Okay")
        }
    }
}