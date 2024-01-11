package com.example.weatherapp

data class DailyForecastModel(
    val date: String,
    val currentConditionIcon: Int,
    val minTemp: String,
    val maxTemp: String
)
