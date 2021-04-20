package io.arunbuilds.covidtracker

import com.google.gson.annotations.SerializedName
import java.util.Date

data class CovidData(
    @SerializedName("dateChecked") val dateChecked: Date,
    @SerializedName("positiveIncrease") val positiveIncrease: Int,
    @SerializedName("negativeIncrease") val negativeIncrease: Int,
    @SerializedName("deathIncrease") val deathIncrease: Int,
    @SerializedName("state") val state: String
)
