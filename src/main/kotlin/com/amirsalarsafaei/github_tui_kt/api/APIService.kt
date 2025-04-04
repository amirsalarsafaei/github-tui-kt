
package com.amirsalarsafaei.github_tui_kt.api
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.ZonedDateTime

object APIService {
    val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
            .setPrettyPrinting()
            .create()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val github: GithubService by lazy {
        retrofit.create(GithubService::class.java)
    }
}
