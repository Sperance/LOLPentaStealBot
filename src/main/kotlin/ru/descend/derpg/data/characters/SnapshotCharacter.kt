package ru.descend.derpg.data.characters

import ru.descend.derpg.test.BaseDTO
import ru.descend.derpg.test.PostMetadata

class SnapshotCharacter(
    val _id: Long,
    var _title: String,
    var _content: String,
    var _metadata: PostMetadata,
    val _userId: Long
) : BaseDTO()