package com.example.weatherapp

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

private const val API_KEY: String = "d18f8eb0c67a4f8bad850325232712"

class WeatherViewModel : ViewModel() {
    private var liveWeatherData = MutableLiveData<WeatherModel>()
    private val liveHourlyForecastData = MutableLiveData<List<HourlyForecastModel>>()
    private val liveDailyForecastData = MutableLiveData<List<DailyForecastModel>>()

    fun getLiveWeatherData() = liveWeatherData
    fun getLiveHourlyForecastData() = liveHourlyForecastData
    fun getLiveDailyForecastData() = liveDailyForecastData

    fun updateLiveData(
        cityOrLatAndLong: String = "Moscow",
        swipeRefreshLayout: SwipeRefreshLayout,
        days: Int = 3,
        aqi: String = "no",
        alerts: String = "no",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            val json: JSONObject = async {
                getJSONObject(
                    "https://api.weatherapi.com/v1/forecast.json?key=$API_KEY&q=$cityOrLatAndLong&days=$days&aqi=$aqi&alerts=$alerts"
                )
            }.await()
            liveWeatherData.value = getWeatherModel(json)
            liveHourlyForecastData.value = getHourlyForecastModel(json)
            liveDailyForecastData.value = getDailyForecastModel(json)
            onComplete()
        }
    }

    private suspend fun getJSONObject(url: String): JSONObject =
        withContext(Dispatchers.IO) { JSONObject(URL(url).readText()) }

    private fun getWeatherModel(json: JSONObject): WeatherModel = WeatherModel(
        json
            .getJSONObject("location")
            .getString("name"),
        getLastUpdated(json),
        json
            .getJSONObject("current")
            .getString("temp_c")
            .dropLast(2) + "°",
        json
            .getJSONObject("current")
            .getJSONObject("condition")
            .getString("text"),
        getCurrentConditionIcon(
            json
                .getJSONObject("current")
                .getJSONObject("condition"),
            json
                .getJSONObject("current")
                .getString("is_day") == "1"
        ),
        json
            .getJSONObject("forecast")
            .getJSONArray("forecastday")
            .getJSONObject(0)
            .getJSONObject("day")
            .getString("mintemp_c")
            .dropLast(2) + "°",
        json
            .getJSONObject("forecast")
            .getJSONArray("forecastday")
            .getJSONObject(0)
            .getJSONObject("day")
            .getString("maxtemp_c")
            .dropLast(2) + "°"
    )


    private fun getLastUpdated(json: JSONObject): String {
        @SuppressLint("SimpleDateFormat")
        val dataFormat = SimpleDateFormat("yyyy-MM-dd H:mm")

        val localLastUpdate =
            dataFormat.parse(
                json
                    .getJSONObject("current")
                    .getString("last_updated")
            )
        val localCurrentTime =
            dataFormat.parse(
                json
                    .getJSONObject("location")
                    .getString("localtime")
            )
        val currentTime: Date = Calendar.getInstance().time

        val difference = Date(currentTime.time - (localCurrentTime!!.time - localLastUpdate!!.time))

        return dataFormat.format(difference)
    }

    private fun getCurrentConditionIcon(json: JSONObject, isDay: Boolean = true): Int =
        when (json.getString("code").toInt()) {
            1000 -> if (isDay) R.drawable.ic_sunny_32dp else R.drawable.ic_clear_night_32dp
            1003 -> if (isDay) R.drawable.ic_partly_cloudy_32dp else R.drawable.ic_partly_cloudy_night_32dp
            1006, 1009 -> R.drawable.ic_cloudy_32dp
            1030, 1135, 1147 -> R.drawable.ic_fog_32dp
            1063, 1180 -> if (isDay) R.drawable.ic_rain_sun_32dp else R.drawable.ic_rain_night_32dp
            1066, 1210, 1213, 1216, 1219, 1222, 1225, 1255, 1258 -> R.drawable.ic_snow_32dp
            1069, 1204, 1207, 1249, 1252 -> R.drawable.ic_sleet_32dp
            1087 -> if (isDay) R.drawable.ic_scatterad_thunderstorm_32dp else R.drawable.ic_sever_thunderstorm_32dp
            1114 -> R.drawable.ic_blowing_snow_32dp
            1117 -> R.drawable.ic_blizzard_32dp
            1150 -> if (isDay) R.drawable.ic_drizzle_sun_32dp else R.drawable.ic_drizzle_night_32dp
            1053, 1072, 1168, 1171 -> R.drawable.ic_drizzle_32dp
            1183, 1186 -> R.drawable.ic_rain_32dp
            1189, 1192, 1195, 1246 -> R.drawable.ic_heavy_rain_32dp
            1198, 1201, 1237, 1261, 1264 -> R.drawable.ic_hail_32dp
            1273, 1276 -> R.drawable.ic_rain_thunderstorm_32dp
            1279, 1282 -> R.drawable.ic_sever_thunderstorm_32dp
            1240, 1243 -> if (isDay) R.drawable.ic_scatterad_showers_32dp else R.drawable.ic_scatterad_showers_night_32dp
            else -> R.drawable.ic_sunny_32dp
        }

    private fun getHourlyForecastModel(json: JSONObject): List<HourlyForecastModel> {
        val hourlyForecastMutableList = mutableListOf<HourlyForecastModel>()
        var currentHour: Int = json
            .getJSONObject("location")
            .getString("localtime")
            .takeLast(5).dropLast(3).trim().toInt()
        var currentDay: Int = 0
        for (i in 1..24) {
            currentHour += 1
            if (currentHour == 24) {
                currentDay = 1
                currentHour = 0
            }
            hourlyForecastMutableList.add(
                HourlyForecastModel(
                    if (currentHour < 10) "0$currentHour:00" else "$currentHour:00",
                    getCurrentConditionIcon(
                        json
                            .getJSONObject("forecast")
                            .getJSONArray("forecastday")
                            .getJSONObject(currentDay)
                            .getJSONArray("hour")
                            .getJSONObject(currentHour)
                            .getJSONObject("condition"),
                        json
                            .getJSONObject("forecast")
                            .getJSONArray("forecastday")
                            .getJSONObject(currentDay)
                            .getJSONArray("hour")
                            .getJSONObject(currentHour)
                            .getString("is_day") == "1"
                    ),
                    json
                        .getJSONObject("forecast")
                        .getJSONArray("forecastday")
                        .getJSONObject(currentDay)
                        .getJSONArray("hour")
                        .getJSONObject(currentHour)
                        .getString("temp_c")
                        .dropLast(2) + "°"
                )
            )
        }
        return hourlyForecastMutableList.toList()
    }

    private fun getDailyForecastModel(json: JSONObject): List<DailyForecastModel> {
        val dailyForecastModel = mutableListOf<DailyForecastModel>()
        for (index in 0..2) {
            dailyForecastModel.add(
                DailyForecastModel(
                    json
                        .getJSONObject("forecast")
                        .getJSONArray("forecastday")
                        .getJSONObject(index)
                        .getString("date"),
                    getCurrentConditionIcon(
                        json
                            .getJSONObject("forecast")
                            .getJSONArray("forecastday")
                            .getJSONObject(index)
                            .getJSONObject("day")
                            .getJSONObject("condition")
                    ),
                    json
                        .getJSONObject("forecast")
                        .getJSONArray("forecastday")
                        .getJSONObject(index)
                        .getJSONObject("day")
                        .getString("mintemp_c")
                        .dropLast(2) + "°",
                    json
                        .getJSONObject("forecast")
                        .getJSONArray("forecastday")
                        .getJSONObject(index)
                        .getJSONObject("day")
                        .getString("maxtemp_c")
                        .dropLast(2) + "°"
                )
            )
        }
        return dailyForecastModel.toList()
    }
}