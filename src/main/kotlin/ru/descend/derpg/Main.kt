package ru.descend.derpg

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.dao.flushCache
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

            characterDao.create {
                title = "First post"
                content = "Hello JSONB!"
                metadata = PostMetadata(
                    tags = arrayListOf(ItemObject("Sword"), ItemObject("Arrow")),
                    isPublished = true
                )
                this.user = user
            }

            characterDao.create {
                title = "Draft post"
                content = "Work in progress"
                metadata = PostMetadata(
                    tags = arrayListOf(ItemObject("Body")),
                    isPublished = false
                )
                this.user = user
            }

            printLog("POSTS 1: ${user.getCharacters()}")

            user.getCharacters().forEachIndexed { ind, it ->

                // Обновляем metadata - СПОСОБ 1: Создаем новый объект
//                val currentMetadata = character.metadata
//                val newTags = currentMetadata.tags.toMutableList().apply {
//                    add(ItemObject("INSERTED $ind"))
//                }
//
//                character.metadata = currentMetadata.copy(
//                    tags = newTags,
//                    isPublished = currentMetadata.isPublished
//                )

                it.title += " $ind"
                it.content += " CCC$ind"

                val curMeta = it.metadata
                val tags = curMeta.tags.toMutableList().apply {
                    add(ItemObject("INSERTED $ind"))
                }
//                curMeta.tags.add(ItemObject("INSERTED $ind"))

                it.metadata = curMeta.copy(
                    tags = tags,
                    isPublished = curMeta.isPublished
                )
            }

            val posts = user.getCharacters()

            printLog("POSTS 2: $posts")
        }
        DatabaseConfig.close()
    }
}