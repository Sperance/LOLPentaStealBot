package ru.descend.bot.savedObj

import ru.descend.bot.printLog

class WorkData<T>(val nameObject: String) {

    private val listData = ArrayList<T>()
    var bodyReset: (suspend () -> List<T>)? = null

    suspend fun get(reset: Boolean = false) : ArrayList<T> {
        if (isEmpty() || reset) reset()
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
        val beforeCounter = listData.size
        listData.clear()
        listData.addAll(bodyReset?.invoke()?: listOf())
        val afterCounter = listData.size
        printLog("[$nameObject::reset] $beforeCounter -> $afterCounter")
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