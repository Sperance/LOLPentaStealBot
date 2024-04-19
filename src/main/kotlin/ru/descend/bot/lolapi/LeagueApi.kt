package ru.descend.bot.lolapi

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.descend.bot.lolapi.services.LLDragonService
import ru.descend.bot.lolapi.services.LeagueService
import java.util.concurrent.TimeUnit

class LeagueApi(private val apiKey: String, region: String) {

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

    private val ENDPOINT: String = "https://$region.api.riotgames.com"
    val dragonService : LLDragonService by lazy { retrofit.create(LLDragonService::class.java) }
    val leagueService : LeagueService by lazy { retrofit.create(LeagueService::class.java) }

    private var retrofit: Retrofit = createRetrofit()

    private fun createRetrofit() : Retrofit {
        return Retrofit.Builder()
            .baseUrl(ENDPOINT)
            .addConverterFactory(GsonConverterFactory.create())
            .client(createOkHttpClient())
            .build()
    }

    private fun createOkHttpClient(): OkHttpClient {
        val okHttpClient = OkHttpClient()
        val newOkHttpBuilder = okHttpClient.newBuilder()
        newOkHttpBuilder.readTimeout(10, TimeUnit.MINUTES)
        newOkHttpBuilder.connectTimeout(10, TimeUnit.MINUTES)
        newOkHttpBuilder.addInterceptor {
            val requestBuilder: Request.Builder = it.request().newBuilder()
            requestBuilder.header("Content-Type", "application/json")
            requestBuilder.header("Origin", "https://developer.riotgames.com")
            requestBuilder.header("X-Riot-Token", apiKey)
            requestBuilder.header("X-RateLimit-Limit", "100")
            requestBuilder.header("X-RateLimit-Period", "120")
            it.proceed(requestBuilder.build())
        }
        return newOkHttpBuilder.build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LeagueApi

        if (apiKey != other.apiKey) return false
        if (ENDPOINT != other.ENDPOINT) return false
        if (dragonService != other.dragonService) return false
        if (leagueService != other.leagueService) return false
        if (retrofit != other.retrofit) return false

        return true
    }

    override fun hashCode(): Int {
        var result = apiKey.hashCode()
        result = 31 * result + ENDPOINT.hashCode()
        result = 31 * result + dragonService.hashCode()
        result = 31 * result + leagueService.hashCode()
        result = 31 * result + retrofit.hashCode()
        return result
    }
}