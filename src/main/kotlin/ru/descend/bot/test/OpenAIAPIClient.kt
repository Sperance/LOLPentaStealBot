package ru.descend.bot.test

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class ChatMessage(val message: String, val isUser: Boolean)

object OpenAIAPIClient {
    private const val BASE_URL = "https://api.openai.com/v1/"

    interface OpenAIAPIService {
        @Headers("Authorization: Bearer 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")
        @POST("chat/completions")
        fun getCompletion(@Body requestModel: OpenAIRequestModel): Call<OpenAIResponseModel>
    }

    fun create(): OpenAIAPIService {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(OpenAIAPIService::class.java)
    }
}