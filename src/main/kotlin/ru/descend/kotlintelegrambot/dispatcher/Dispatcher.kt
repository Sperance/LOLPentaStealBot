package ru.descend.kotlintelegrambot.dispatcher

import ru.descend.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.ErrorHandler
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.errors.TelegramError
import ru.descend.kotlintelegrambot.types.DispatchableObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class Dispatcher internal constructor(
    private val updatesChannel: Channel<DispatchableObject>,
    coroutineDispatcher: CoroutineDispatcher,
) {

    internal lateinit var bot: Bot

    private val commandHandlers = linkedSetOf<Handler>()
    private val errorHandlers = arrayListOf<ErrorHandler>()

    private val scope: CoroutineScope = CoroutineScope(coroutineDispatcher)

    @Volatile private var job: Job? = null

    internal fun startCheckingUpdates() {
        job?.cancel()
        job = scope.launch { checkQueueUpdates() }
    }

    private suspend fun checkQueueUpdates() {
        while (true) {
            when (val item = updatesChannel.receive()) {
                is Update -> handleUpdate(item)
                is TelegramError -> handleError(item)
                else -> Unit
            }
            yield()
        }
    }

    fun addHandler(handler: Handler) {
        commandHandlers.add(handler)
    }

    fun removeHandler(handler: Handler) {
        commandHandlers.remove(handler)
    }

    fun addErrorHandler(errorHandler: ErrorHandler) {
        errorHandlers.add(errorHandler)
    }

    fun removeErrorHandler(errorHandler: ErrorHandler) {
        errorHandlers.remove(errorHandler)
    }

    private suspend fun handleUpdate(update: Update) {
        commandHandlers
            .asSequence()
            .filter { !update.consumed }
            .filter { it.checkUpdate(update) }
            .forEach {
                try {
                    it.handleUpdate(bot, update)
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
    }

    private fun handleError(error: TelegramError) {
        errorHandlers.forEach { handleError ->
            try {
                handleError(bot, error)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
        }
    }

    internal fun stopCheckingUpdates() {
        job?.cancel()
    }
}
