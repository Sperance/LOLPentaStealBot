# Commands

## Key 
| Symbol      | Meaning                        |
|-------------|--------------------------------|
| [Argument]  | Argument is not required.      |
| /Category   | This is a subcommand group.    |

## /Prompt
| Commands | Arguments | Description                                                          |
|----------|-----------|----------------------------------------------------------------------|
| Account  |           | Ввести информацию для Бота по текущему аккаунту пользователя Discord |

## Arguments
| Commands            | Arguments                  | Description                                                               |
|---------------------|----------------------------|---------------------------------------------------------------------------|
| addDonation         | user, gold                 | Добавить донат к пользователю                                             |
| addSavedMMR         | user, savedMMR             | Добавить бонусные MMR пользователю                                        |
| clearDebugChannel   |                            | Очистка канала для системных сообщений                                    |
| clearMainChannel    |                            | Очистка основного канал для бота                                          |
| clearStatusChannel  |                            | Очистка канала для сообщений бота                                         |
| genText             | request                    | Получить ответ от ChatGPT на запрос                                       |
| genTextAdmin        | request                    |                                                                           |
| getAlliesWinrate    |                            | Просмотр Винрейта каждого игрока сервера (в боте) по отношению к себе     |
| getChampionsWinrate |                            | Просмотр Винрейта по всем своим сыгранным чемпионам за последние 30 дней  |
| initDebugChannel    | channel                    | Канал для системных сообщений                                             |
| initMainChannel     | channel                    | Основной канал для бота                                                   |
| initStatusChannel   | channel                    | Канал для сообщений бота                                                  |
| removeSavedMMR      | user, savedMMR             | Вычесть бонусные MMR пользователю                                         |
| setBirthdayDate     | user, date                 | Ввести дату рождения пользователя (в формате ddmmyyyy, например 03091990) |
| userCreate          | User, Region, SummonerName | Создание учетной записи Лиги легенд и пользователя Discord                |
| userDelete          | User                       | Удалить учётную запись из базы данных бота                                |
| userDeleteFromID    | id                         | Удалить учётную запись из базы данных бота по ID                          |

## Utility
| Commands | Arguments | Description                   |
|----------|-----------|-------------------------------|
| Help     | [Command] | Display a help menu.          |
| info     |           | Bot info for LOLPentaStealBot |

