package ru.descend.bot

/**
 * Каждый N тик прогрузки матчей будет проходиться по сохранённым пользователям
 */
const val EVERY_N_TICK_LOAD_MATCH = 2

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
const val LOAD_SAVED_USER_MATCHES = 100

/**
 * Каждое указанное кол-во матчей будет прогрузка таблицы ММР и Чемпионов
 */
const val LOAD_MMR_HEROES_MATCHES = 5000

/**
 * Сколько матчей будет загружено по каждому неизвестному пользователю когда загружен матч по известному
 */
const val LOAD_MATCHES_ON_SAVED_UNDEFINED = 5

const val MVP_TAG = "MVP"
/**
 * Бонусных ММР за получение ранга MVP в АРАМе
 */
const val BONUS_MMR_FOR_MVP_ARAM = 2.0

const val LVP_TAG = "LVP"
/**
 * Штрафных ММР за получение ранга LVP в АРАМе
 */
const val BONUS_MMR_FOR_LVP_ARAM = 2.0

/**
 * Бонусных ММР за победу
 */
const val BONUS_MMR_FOR_WIN = 0.5

/**
 * Бонусные ММР за получение нового ранга
 */
const val BONUS_MMR_FOR_NEW_RANK = 10.0

/**
 * Максимальное возможное кол-во получения бонусных ММР за матч
 */
const val LIMIT_BINUS_MMR_FOR_MATCH = 10.0

/**
 * Кол-во дней для прогрузки матчей каждого пользователя (было 60)
 */
const val DAYS_MIN_IN_LOAD = 30L

/**
 * Модификатор для ММР
 */
const val MMR_STOCK_MODIFICATOR = 1.2

/**
 * Сколько ММР необходимо добавить к Максимальному ММР для подсчёта ММР для проигравшей команды
 */
const val ADD_MMR_FOR_LOOSE_ARAM_CALC = 2.0