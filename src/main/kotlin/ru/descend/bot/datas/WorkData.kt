package ru.descend.bot.datas

import ru.descend.bot.launch
import ru.descend.bot.printLog

class WorkData<T>(private val nameObject: String) {

    private val listData = ArrayList<T>()
    var bodyReset: (suspend () -> List<T>)? = null

    suspend fun get(reset: Boolean = false) : ArrayList<T> {
        if (isEmpty() || reset) {
            launch {
                reset()
            }.join()
        }
        return listData
    }

    fun set(list: List<T>) {
        clear()
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
        clear()
        listData.addAll(bodyReset?.invoke()?: listOf())
        if (listData.isEmpty()) {
            printLog("[WorkData::$nameObject] reseted to Empty list")
        }
    }
}