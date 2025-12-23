package ru.descend.derpg.data.users

import ru.descend.derpg.data.characters.SnapshotCharacter
import ru.descend.derpg.test.BaseDTO

class SnapshotUser(
    val _id: Long,
    var _name: String,
    var _email: String
) : BaseDTO() {
    fun getCharacters(dao: DAOusers): List<SnapshotCharacter> {
        return dao.findById(_id)?.getCharacters()?.map { it.toSnapshot() }?:listOf()
    }
}