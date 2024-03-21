package ru.descend.bot.postgre.r2dbc.interfaces

interface InterfaceR2DBC<T> {
    suspend fun save() : T
    suspend fun update(): T
    suspend fun delete()
}