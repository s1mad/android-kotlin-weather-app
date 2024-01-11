package com.example.weatherapp

data class HourlyForecastModel(
    val time: String,
    val currentConditionIcon: Int,
    val temp: String
)
