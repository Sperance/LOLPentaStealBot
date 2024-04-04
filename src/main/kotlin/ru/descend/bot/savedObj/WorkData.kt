package ru.descend.bot.savedObj

class WorkData<T> {

    private val listData = ArrayList<T>()
    var bodyReset: (suspend () -> List<T>)? = null

    suspend fun get(reset: Boolean = false) : ArrayList<T> {
        if (isEmpty() || reset) reset()
        return listData
    }

    fun getSize() = listData.size

    fun set(list: List<T>) {
        listData.clear()
        listData.addAll(list)
    }

    fun add(value: T) {
        listData.add(value)
    }

    fun clear() {
        listData.clear()
    }

    private fun isEmpty() = listData.isEmpty()
    suspend fun reset() {
        listData.clear()
        listData.addAll(bodyReset?.invoke()?: listOf())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorkData<*>

        if (listData != other.listData) return false
        if (bodyReset != other.bodyReset) return false

        return true
    }

    override fun hashCode(): Int {
        var result = listData.hashCode()
        result = 31 * result + (bodyReset?.hashCode() ?: 0)
        return result
    }
}