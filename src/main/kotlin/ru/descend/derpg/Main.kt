package ru.descend.derpg

import kotlinx.coroutines.runBlocking
import ru.descend.bot.printLog
import ru.descend.derpg.DatabaseConfig.dbQuery
import ru.descend.derpg.data.characters.DAOCharacters
import ru.descend.derpg.data.users.DAOusers
import ru.descend.derpg.test.ItemObject
import ru.descend.derpg.test.PostMetadata

fun main() {
    runBlocking {
        DatabaseConfig.init()

        val userDao = DAOusers()
        val characterDao = DAOCharacters()
        dbQuery {
            val user = userDao.create {
                name = "John${System.currentTimeMillis()}"
                email = "john@example.com"
            }

            userDao.update(user) {
                email = "CHANGED EMILE1"
            }

            userDao.update(user) {
                email = "CHANGED EMILE2"
            }

            // создаём пару постов с jsonb-метаданными
            characterDao.create {
                this.user = user
                title = "First post"
                content = "Hello JSONB!"
                metadata = PostMetadata(
                    tags = listOf(ItemObject("Sword"), ItemObject("Arrow")),
                    isPublished = true
                )
            }

            characterDao.create {
                this.user = user
                title = "Draft post"
                content = "Work in progress"
                metadata = PostMetadata(
                    tags = listOf(ItemObject("Body"), ItemObject("")),
                    isPublished = false
                )
            }

            printLog("POSTS 1: ${user.getCharacters()}")


//            val loaded = userDao.findById(user.id)!!
            val posts = user.getCharacters()   // one-to-many

            printLog("POSTS 2: $posts")
        }
        DatabaseConfig.close()
    }
}