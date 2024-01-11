package com.example.weatherapp

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var hourlyForecastItemAdapter: HourlyForecastItemAdapter
    private lateinit var dailyForecastItemAdapter: DailyForecastItemAdapter

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hourlyForecastItemAdapter = HourlyForecastItemAdapter(this, emptyList())
        binding.hourlyForecastRecyclerView.adapter = hourlyForecastItemAdapter

        dailyForecastItemAdapter = DailyForecastItemAdapter(this, emptyList())
        binding.dailyForecastRecyclerView.adapter = dailyForecastItemAdapter

        val viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]

        binding.swipeRefreshLayout.isRefreshing = true
        viewModel.updateLiveData {
            binding.swipeRefreshLayout.isRefreshing = false
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.updateLiveData() {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        viewModel.getLiveWeatherData().observe(this, Observer {
            with(binding) {
                currentLocationTextView.text = it.city
                lastUpdateTimeTextView.text = it.lastUpdated.takeLast(5)
                conditionWeatherTextView.text = it.currentCondition
                weatherIconImageView.setImageResource(it.currentConditionIcon)
                currentTemperatureTextView.text = it.currentTemp
                minMaxTemperatureTextView.text = "${it.minTemp}/${it.maxTemp}"
            }
        })

        viewModel.getLiveHourlyForecastData().observe(this, Observer {
            hourlyForecastItemAdapter.updateHourlyForecastList(it)
        })

        viewModel.getLiveDailyForecastData().observe(this, Observer {
            dailyForecastItemAdapter.updateDailyForecastList(it)
        })
    }
}