package io.arunbuilds.covidtracker

import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.ticker.TickerUtils
import io.arunbuilds.covidtracker.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val ALL_STATES: String = " All (Nationwide) "
    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var nationalDailyData: List<CovidData>
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private val TAG = MainActivity::class.java.simpleName
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)


        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.covidtracking.com/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val covidService = retrofit.create(CovidService::class.java)
        // fetch the national data
        covidService.getNationalData().enqueue(object : Callback<List<CovidData>> {
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG, "onResponse $response")
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.w(TAG, "Didnt receive valid response body")
                    return
                }
                setUpEventListeners()
                nationalDailyData = nationalData.reversed()
                Log.i(TAG, nationalDailyData.toString())
                updateDisplayWithData(nationalDailyData)
            }

        })

        // fetch the state data
        covidService.getStatesData().enqueue(object : Callback<List<CovidData>> {
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG, "onResponse $response")
                val statesData = response.body()
                if (statesData == null) {
                    Log.w(TAG, "Didnt receive valid response body")
                    return
                }
                perStateDailyData = statesData.reversed().groupBy { it.state }
                Log.i(TAG, nationalDailyData.toString())

                updateSpinnerWithStateData(perStateDailyData.keys)
            }

        })
    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        val stateAbbreviationList = stateNames.toMutableList()
        stateAbbreviationList.sort()
        stateAbbreviationList.add(0, ALL_STATES)

        //add state list data source for the spinner
        binding.niceSpinner.attachDataSource(stateAbbreviationList)
        binding.niceSpinner.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val selectedState = parent.getItemAtPosition(position) as String
            val selectedData = perStateDailyData[selectedState] ?: nationalDailyData
            updateDisplayWithData(selectedData)

        }
    }

    private fun setUpEventListeners() {
        // add a listener for user scrubbing on the chart
        with(binding) {
            sparkView2.isScrubEnabled = true
            sparkView2.setScrubListener { covidData ->
                if (covidData is CovidData) {
                    updateInfoForDate(covidData)
                }
            }
            // respond to radio button
            radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedId ->
                adapter.daysAgo = when (checkedId) {
                    R.id.radioButtonWeek -> TimeScale.WEEK
                    R.id.radioButtonMonth -> TimeScale.MONTH
                    else -> TimeScale.MAX
                }
                adapter.notifyDataSetChanged()
            }

            radioGroupMetricsSelection.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.radioButtonNegative -> updateDisplayMetric(Metric.NEGATIVE)
                    R.id.radioButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
                    else -> updateDisplayMetric(Metric.DEATH)
                }
            }
            tickerView.setCharacterLists(TickerUtils.provideNumberList())

        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        //update color
        val color = when (metric) {
            Metric.NEGATIVE -> R.color.colorNegative
            Metric.POSITIVE -> R.color.colorPositive
            Metric.DEATH -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this, color)
        binding.sparkView2.lineColor = colorInt
        binding.tickerView.textColor = colorInt

        //update metric on the adapter
        adapter.metric = metric
        adapter.notifyDataSetChanged()

        //reset number and date shown in bottom text views
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
        //Create Spark Adapter
        adapter = CovidSparkAdapter(dailyData)
        binding.sparkView2.adapter = adapter

        //Update radio button to select positive case and max time line
        binding.radioButtonPositive.isChecked = true
        binding.radioButtonMax.isChecked = true

        //display metric for most recent data
        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when (adapter.metric) {
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        binding.tickerView.text = NumberFormat.getInstance().format(numCases)
        val date = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
        val dateString = date.format(covidData.dateChecked)
        binding.tvDateLabel.text = dateString
    }
}