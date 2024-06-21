package ru.descend.bot

/**
 * Каждый N тик прогрузки матчей будет проходиться по сохранённым пользователям
 */
const val EVERY_N_TICK_LOAD_MATCH = 3

/**
 * Сколько матчей загружаем по каждому неизвестному пользователю
 */
const val LOAD_MATCHES_IN_USER = 10

/**
 * Количество неизвестных последних пользователей по которым работает авто-загрузка матчей
 */
const val LAST_UNDEFINED_USERS_FOR_LOAD = 20

/**
 * Количество матчей постоянно прогружаемых у сохраненных пользователей
 */
const val LOAD_SAVED_USER_MATCHES = 50

/**
 * Каждое указанное кол-во матчей будет прогрузка таблицы ММР и Чемпионов
 */
const val LOAD_MMR_HEROES_MATCHES = 10000

/**
 * Сколько матчей будет загружено по каждому неизвестному пользователю когда загружен матч по известному
 */
const val LOAD_MATCHES_ON_SAVED_UNDEFINED = 5

/**
 * Бонусных ММР за получение ранга MVP в АРАМе
 */
const val BONUS_MMR_FOR_MVP_ARAM = 2.0

/**
 * Штрафных ММР за получение ранга LVP в АРАМе
 */
const val BONUS_MMR_FOR_LVP_ARAM = 2.0