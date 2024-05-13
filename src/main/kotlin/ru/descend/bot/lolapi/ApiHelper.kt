package ru.descend.bot.lolapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

suspend fun <T : Any> safeApiCall(
    call: suspend () -> Response<T>
): Result<T> {
    return withContext(Dispatchers.IO) {
        try {
            val response = call.invoke()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.Success(body)
                } else {
                    Result.Error("Response body is null", response.code())
                }
            } else {
                Result.Error("Error response: ${response.code()} ${response.message()}", response.code())
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.localizedMessage}", -1)
        }
    }
}

sealed class Result<out T : Any> {
    data class Success<out T : Any>(val data: T) : Result<T>()
    data class Error(val message: String, val errorCode: Int) : Result<Nothing>()
}