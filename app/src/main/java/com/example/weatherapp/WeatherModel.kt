package com.example.weatherapp

data class WeatherModel(
    val city: String,
    val lastUpdated: String,
    val currentTemp: String,
    val currentCondition: String,
    val currentConditionIcon: Int,
    val minTemp: String,
    val maxTemp: String
)
