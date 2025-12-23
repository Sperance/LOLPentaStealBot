package ru.descend.derpg.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.json.jsonb

/**************/



@Serializable
data class PostMetadata(
    val tags: List<ItemObject> = emptyList(),
    val isPublished: Boolean = false
) {
    override fun toString(): String {
        return "PostMetadata(tags=$tags, isPublished=$isPublished)"
    }
}

@Serializable
data class ItemObject(
    val name: String,
    val stats: String = "stats"
) {
    override fun toString(): String {
        return "ItemObject(name='$name', stats='$stats')"
    }
}