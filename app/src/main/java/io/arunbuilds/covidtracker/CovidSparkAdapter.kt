package io.arunbuilds.covidtracker

import android.graphics.RectF
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(private val dailyData: List<CovidData>) : SparkAdapter() {

    var metric: Metric = Metric.POSITIVE
    var daysAgo: TimeScale = TimeScale.MAX

    override fun getY(index: Int): Float {
        val chosenDayData = dailyData[index]
        return when (metric) {
            Metric.POSITIVE -> chosenDayData.positiveIncrease.toFloat()
            Metric.NEGATIVE -> chosenDayData.negativeIncrease.toFloat()
            Metric.DEATH -> chosenDayData.deathIncrease.toFloat()
        }
    }

    override fun getItem(index: Int) = dailyData[index]

    override fun getCount() = dailyData.size

    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if(daysAgo != TimeScale.MAX) bounds.left = count - daysAgo.numDays.toFloat()
        return bounds
    }
}
