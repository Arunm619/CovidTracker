package io.arunbuilds.covidtracker

import retrofit2.Call
import retrofit2.http.GET

interface CovidService {

    @GET("v1/us/daily.json")
    fun getNationalData(): Call<List<CovidData>>

    @GET("v1/states/daily.json")
    fun getStatesData(): Call<List<CovidData>>
}