package com.example.weather.Utilities

import com.example.weather.models.CurrentWeather
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    @GET("weather?")
    fun getCurrentWeatherData(
        @Query("lat") lat:String,
        @Query("lon") lon:String,
        @Query("Appid") apiKey:String
    ): Call<CurrentWeather>
    @GET("weather?")
    fun getCityWeatherData(
        @Query("q")q:String,
        @Query("Appid")apiKey:String
    ):Call<CurrentWeather>
}