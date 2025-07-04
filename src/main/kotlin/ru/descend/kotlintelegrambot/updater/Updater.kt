package ru.descend.kotlintelegrambot.updater

import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.errors.RetrieveUpdatesError
import ru.descend.kotlintelegrambot.network.ApiClient
import ru.descend.kotlintelegrambot.types.DispatchableObject
import ru.descend.kotlintelegrambot.types.TelegramBotResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.yield

internal class Updater(
    private val looper: Looper,
    private val updatesChannel: Channel<DispatchableObject>,
    private val apiClient: ApiClient,
    private val botTimeout: Int,
) {

    @Volatile private var lastUpdateId: Long? = null

    internal fun startPolling() {
        looper.loop {
            val getUpdatesResult = apiClient.getUpdates(
                offset = lastUpdateId,
                limit = null,
                timeout = botTimeout,
                allowedUpdates = null,
            )
            yield()
            getUpdatesResult.fold(
                ifSuccess = { onUpdatesReceived(it) },
                ifError = { onErrorGettingUpdates(it) },
            )
        }
    }

    internal fun stopPolling() {
        looper.quit()
    }

    private suspend fun onUpdatesReceived(updates: List<Update>) {
        if (updates.isEmpty()) {
            return
        }

        updates.forEach {
            updatesChannel.send(it)
        }

        lastUpdateId = updates.last().updateId + 1
    }

    private suspend fun onErrorGettingUpdates(error: TelegramBotResult.Error) {
        val errorDescription: String? = when (error) {
            is TelegramBotResult.Error.HttpError -> "${error.httpCode} ${error.description}"
            is TelegramBotResult.Error.TelegramApi -> "${error.errorCode} ${error.description}"
            is TelegramBotResult.Error.InvalidResponse -> "${error.httpCode} ${error.httpStatusMessage}"
            is TelegramBotResult.Error.Unknown -> error.exception.message
        }

        val dispatchableError = RetrieveUpdatesError(
            errorDescription ?: "Error retrieving updates",
        )
        updatesChannel.send(dispatchableError)
    }
}
