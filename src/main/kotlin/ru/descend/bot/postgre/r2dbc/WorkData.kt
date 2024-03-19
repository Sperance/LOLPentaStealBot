package ru.descend.bot.postgre.r2dbc

class WorkData<T> {

    private val listData = ArrayList<T>()
    var bodyReset: (suspend () -> List<T>)? = null

    suspend fun get() : ArrayList<T> {
        if (isEmpty()) reset()
        return listData
    }

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
}