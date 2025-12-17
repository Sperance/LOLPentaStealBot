package ru.descend.derpg.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.json.jsonb
import java.time.Instant

class UserSnapshot(
    val _id: Long,
    var _name: String,
    var _email: String,
    _createdAt: Instant,
    _updatedAt: Instant,
    _deletedAt: Instant?,
    _version: Long
) : BaseDTO(
    _createdAt = _createdAt,
    _updatedAt = _updatedAt,
    _deletedAt = _deletedAt,
    _version = _version
)

object UsersTable : BaseTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255)
}

class UserEntity(id: EntityID<Long>) : BaseEntity<UserSnapshot>(id, UsersTable) {
    var name by UsersTable.name
    var email by UsersTable.email
    val posts by PostEntity referrersOn PostsTable.user

    override fun toSnapshot(): UserSnapshot =
        UserSnapshot(
            _id = id.value,
            _name = name,
            _email = email,
            _createdAt = createdAt,
            _updatedAt = updatedAt,
            _deletedAt = deletedAt,
            _version = version
        )

    companion object : LongEntityClass<UserEntity>(UsersTable)
}

class UserDao : ExposedBaseDao<UsersTable, UserEntity>(
    UsersTable,
    UserEntity
) {
    override fun applyEntityToStatement(entity: UserEntity, stmt: UpdateStatement) {
        stmt[table.name] = entity.name
        stmt[table.email] = entity.email
    }
}

/**************/

object PostsTable : BaseTable("posts") {
    val user = reference("user_id", UsersTable)
    val title = varchar("title", 255)
    val content = text("content")

    val metadata = jsonb<PostMetadata>(
        name = "metadata",
        jsonConfig = Json,
//        jsonConfig = Json { this.prettyPrint = true ; this.encodeDefaults = true },
        kSerializer = PostMetadata.serializer()
    )
}

class PostEntity(id: EntityID<Long>) : BaseEntity<PostSnapshot>(id, PostsTable) {
    var user by UserEntity referencedOn PostsTable.user
    var title by PostsTable.title
    var content by PostsTable.content
    var metadata by PostsTable.metadata

    override fun toSnapshot(): PostSnapshot =
        PostSnapshot(
            _id = id.value,
            _title = title,
            _content = content,
            _metadata = metadata,
            _userId = user.id.value,
            _createdAt = createdAt,
            _updatedAt = updatedAt,
            _deletedAt = deletedAt,
            _version = version
        )

    override fun toString(): String {
        return "PostEntity(user=$user, title='$title', content='$content', metadata=$metadata)"
    }

    companion object : LongEntityClass<PostEntity>(PostsTable)
}

class PostSnapshot(
    val _id: Long,
    var _title: String,
    var _content: String,
    var _metadata: PostMetadata,
    val _userId: Long,
    _createdAt: Instant,
    _updatedAt: Instant,
    _deletedAt: Instant?,
    _version: Long
) : BaseDTO(_createdAt, _updatedAt, _deletedAt, _version)

class PostDao : ExposedBaseDao<PostsTable, PostEntity>(
    PostsTable,
    PostEntity
) {
    override fun applyEntityToStatement(entity: PostEntity, stmt: UpdateStatement) {
        stmt[table.user] = entity.user.id
        stmt[table.title] = entity.title
        stmt[table.content] = entity.content
        stmt[table.metadata] = entity.metadata          // jsonb [web:113][web:119]
    }
}

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