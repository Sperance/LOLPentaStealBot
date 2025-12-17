package ru.descend.derpg

import kotlinx.coroutines.runBlocking
import ru.descend.bot.printLog
import ru.descend.derpg.DatabaseConfig.dbQuery
import ru.descend.derpg.test.ItemObject
import ru.descend.derpg.test.PostDao
import ru.descend.derpg.test.PostMetadata
import ru.descend.derpg.test.UserDao

fun main() {
    runBlocking {
        DatabaseConfig.init()

        val userDao = UserDao()
        val postDao = PostDao()
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
            postDao.create {
                this.user = user
                title = "First post"
                content = "Hello JSONB!"
                metadata = PostMetadata(
                    tags = listOf(ItemObject("Sword"), ItemObject("Arrow")),
                    isPublished = true
                )
            }

            postDao.create {
                this.user = user
                title = "Draft post"
                content = "Work in progress"
                metadata = PostMetadata(
                    tags = listOf(ItemObject("Body"), ItemObject("")),
                    isPublished = false
                )
            }

            printLog("POSTS 1: ${user.posts.toList()}")

            // читаем пользователя и его посты
            val loaded = userDao.findById(user.id)!!
            val posts = loaded.posts.toList()   // one-to-many

            printLog("POSTS 2: $posts")
        }
        DatabaseConfig.close()
    }
}