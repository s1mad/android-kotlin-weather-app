package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.UnknownHostException
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
        context: Context,
        swipeRefreshLayout: SwipeRefreshLayout,
        cityOrLatAndLong: String = "Moscow",
        days: Int = 3,
        aqi: String = "no",
        alerts: String = "no",
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            val result: Result<JSONObject> = try {
                val json = getJSONObject(
                    "https://api.weatherapi.com/v1/forecast.json?key=$API_KEY&q=$cityOrLatAndLong&days=$days&aqi=$aqi&alerts=$alerts"
                )
                Result.success(json)
            } catch (e: Exception) {
                Result.failure(e)
            }

            result.onSuccess { json ->
                liveWeatherData.value = getWeatherModel(json)
                liveHourlyForecastData.value = getHourlyForecastModel(json)
                liveDailyForecastData.value = getDailyForecastModel(json)

                swipeRefreshLayout.isRefreshing = false

                saveWeatherModelToSharedPreferences(context, json.toString())
            }

            result.onFailure { e ->
                if (e is UnknownHostException) {
                    AlertDialog
                        .Builder(context)
                        .setTitle("Network Error")
                        .setMessage(e.message)
                        .setPositiveButton("Continue") { dialog, _ ->
                            dialog.cancel()
                        }.show()
                    swipeRefreshLayout.isRefreshing = false
                } else {
                    AlertDialog.Builder(context)
                        .setTitle("Error")
                        .setMessage(e.message)
                        .setPositiveButton("Continue") { dialog, _ ->
                            dialog.cancel()
                        }.show()
                    swipeRefreshLayout.isRefreshing = false
                }
            }
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
            .substringBefore(".") + "°",
        getCurrentCondition(
            json
                .getJSONObject("current")
                .getJSONObject("condition"),
            json
                .getJSONObject("current")
                .getString("is_day") == "1"
        ),
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
            .substringBefore(".") + "°",
        json
            .getJSONObject("forecast")
            .getJSONArray("forecastday")
            .getJSONObject(0)
            .getJSONObject("day")
            .getString("maxtemp_c")
            .substringBefore(".") + "°"
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

    private fun getCurrentCondition(json: JSONObject, isDay: Boolean = true): Int =
        when (json.getString("code").toInt()) {
            1000 -> if (isDay) R.string.condition_sunny else R.string.condition_clear
            1003 -> R.string.condition_partly_cloudy
            1006 -> R.string.condition_cloudy
            1009 -> R.string.condition_overcast
            1030 -> R.string.condition_mist
            1063 -> R.string.condition_patchy_rain_nearby
            1066 -> R.string.condition_patchy_snow_nearby
            1069 -> R.string.condition_patchy_sleet_nearby
            1072 -> R.string.condition_patchy_freezing_drizzle_nearby
            1087 -> R.string.condition_thundery_outbreaks_in_nearby
            1114 -> R.string.condition_blowing_snow
            1117 -> R.string.condition_blizzard
            1135 -> R.string.condition_fog
            1147 -> R.string.condition_freezing_fog
            1150 -> R.string.condition_patchy_light_drizzle
            1153 -> R.string.condition_light_drizzle
            1168 -> R.string.condition_freezing_drizzle
            1171 -> R.string.condition_heavy_freezing_drizzle
            1180 -> R.string.condition_patchy_light_rain
            1183 -> R.string.condition_light_rain
            1186 -> R.string.condition_moderate_rain_at_times
            1189 -> R.string.condition_moderate_rain
            1192 -> R.string.condition_heavy_rain_at_times
            1195 -> R.string.condition_heavy_rain
            1198 -> R.string.condition_light_freezing_rain
            1201 -> R.string.condition_moderate_or_heavy_freezing_rain
            1204 -> R.string.condition_light_sleet
            1207 -> R.string.condition_moderate_or_heavy_sleet
            1210 -> R.string.condition_patchy_light_snow
            1213 -> R.string.condition_light_snow
            1216 -> R.string.condition_patchy_moderate_snow
            1219 -> R.string.condition_moderate_snow
            1222 -> R.string.condition_patchy_heavy_snow
            1225 -> R.string.condition_heavy_snow
            1237 -> R.string.condition_ice_pellets
            1240 -> R.string.condition_light_rain_shower
            1243 -> R.string.condition_moderate_or_heavy_rain_shower
            1246 -> R.string.condition_torrential_rain_shower
            1249 -> R.string.condition_light_sleet_showers
            1252 -> R.string.condition_moderate_or_heavy_sleet_showers
            1255 -> R.string.condition_light_snow_showers
            1258 -> R.string.condition_moderate_or_heavy_snow_showers
            1261 -> R.string.condition_light_showers_of_ice_pellets
            1264 -> R.string.condition_moderate_or_heavy_showers_of_ice_pellets
            1273 -> R.string.condition_patchy_light_rain_in_area_with_thunder
            1276 -> R.string.condition_moderate_or_heavy_rain_in_area_with_thunder
            1279 -> R.string.condition_patchy_light_snow_in_area_with_thunder
            1282 -> R.string.condition_moderate_or_heavy_snow_in_area_with_thunder
            else -> R.string.condition_sunny
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
                        .substringBefore(".") + "°"
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
                        .substringBefore(".") + "°",
                    json
                        .getJSONObject("forecast")
                        .getJSONArray("forecastday")
                        .getJSONObject(index)
                        .getJSONObject("day")
                        .getString("maxtemp_c")
                        .substringBefore(".") + "°"
                )
            )
        }
        return dailyForecastModel.toList()
    }

    private fun saveWeatherModelToSharedPreferences(context: Context, json: String) {
        val sharedPreferences = context.getSharedPreferences("WeatherData", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.apply {
            putString("WeatherModel", json)
            apply()
        }
    }

    fun retrieveWeatherModelFromSharedPreferences(context: Context) {
        val sharedPreferences = context.getSharedPreferences("WeatherData", Context.MODE_PRIVATE)
        try {
            sharedPreferences.getString("WeatherModel", null)?.let {
                val pastJSON = JSONObject(it)
                liveWeatherData.value = getWeatherModel(pastJSON)
                liveHourlyForecastData.value = getHourlyForecastModel(pastJSON)
                liveDailyForecastData.value = getDailyForecastModel(pastJSON)
            }
        } catch (e: Exception) {
            Log.e("Error", e.message.toString())
        }

    }
}