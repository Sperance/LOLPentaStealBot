package ru.descend.bot.lolapi

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class LeagueApi(private val apiKey: String, private val region: String) {

    companion object Regions {
        const val BR = "br1"
        const val EUNE = "eun1"
        const val EUW = "euw1"
        const val JP = "jp1"
        const val KR = "kr"
        const val LAN = "la1"
        const val LAS = "la2"
        const val NA = "na1"
        const val OCE = "oc1"
        const val TR = "tr1"
        const val RU = "ru"
        const val PBE = "pbe1"
    }

    val ENDPOINT: String = "https://$region.api.riotgames.com"

    val dragonService : LLDragonService by lazy { retrofit.create(LLDragonService::class.java) }

    var retrofit: Retrofit

    init {
        retrofit = createRetrofit(createOkHttpClient())
    }

    private fun createRetrofit(httpClient: OkHttpClient) : Retrofit {
        return Retrofit.Builder()
            .baseUrl(ENDPOINT)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
    }

    private fun createOkHttpClient(): OkHttpClient {
        val okHttpClient = OkHttpClient()
        val newOkHttpBuilder = okHttpClient.newBuilder()
        newOkHttpBuilder.addInterceptor {
            val requestBuilder: Request.Builder = it.request().newBuilder()
            requestBuilder.header("Content-Type", "application/json")
            requestBuilder.header("Origin", "https://developer.riotgames.com")
            requestBuilder.header("X-Riot-Token", apiKey)

            it.proceed(requestBuilder.build())
        }
        return newOkHttpBuilder.build()
    }
}