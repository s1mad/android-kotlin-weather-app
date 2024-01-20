package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

const val REQUEST_LOCATION_PERMISSION_CODE = 1

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var hourlyForecastItemAdapter: HourlyForecastItemAdapter
    private lateinit var dailyForecastItemAdapter: DailyForecastItemAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: WeatherViewModel

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hourlyForecastItemAdapter = HourlyForecastItemAdapter(this, emptyList())
        binding.hourlyForecastRecyclerView.adapter = hourlyForecastItemAdapter

        dailyForecastItemAdapter = DailyForecastItemAdapter(this, emptyList())
        binding.dailyForecastRecyclerView.adapter = dailyForecastItemAdapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]
        viewModel.retrieveWeatherModelFromSharedPreferences(this)

        checkAndUpdateLiveData(this)

        binding.swipeRefreshLayout.setOnRefreshListener {
            checkAndUpdateLiveData(this)
        }

        viewModel.getLiveWeatherData().observe(this) {
            with(binding) {
                currentLocationTextView.text = it.city
                lastUpdateTimeTextView.text = it.lastUpdated.takeLast(5)
                conditionWeatherTextView.setText(it.currentCondition)
                weatherIconImageView.setImageResource(it.currentConditionIcon)
                currentTemperatureTextView.text = it.currentTemp
                minMaxTemperatureTextView.text = "${it.minTemp}/${it.maxTemp}"
            }
        }

        viewModel.getLiveHourlyForecastData().observe(this) {
            hourlyForecastItemAdapter.updateHourlyForecastList(it)
        }

        viewModel.getLiveDailyForecastData().observe(this) {
            dailyForecastItemAdapter.updateDailyForecastList(it)
        }
    }

    private fun checkAndUpdateLiveData(context: Context) {
        binding.swipeRefreshLayout.isRefreshing = true
        if (!isPermissionGranted()) {
            requestLocationPermission(this, this)
        } else if (!isOnline(context)) {
            AlertDialog
                .Builder(context)
                .setTitle("No Internet connection. Please turn it on, and then click \"Continue\"")
                .setPositiveButton("Continue") { dialog, _ ->
                    dialog.dismiss()
                    if (!isOnline(context)) {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(
                            context,
                            "You didn't turn on the Internet.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }.show()
        } else if (!isLocationEnabled(context)) {
            AlertDialog
                .Builder(context)
                .setTitle("No connection to GPS. Please turn it on, and then click \"Continue\"")
                .setPositiveButton("Continue") { dialog, _ ->
                    dialog.dismiss()
                    if (!isLocationEnabled(context)) {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(
                            context,
                            "You didn't turn on the Internet.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }.show()
        } else {
            getLastKnownLocation {
                when (it) {
                    null -> {
                        AlertDialog
                            .Builder(context)
                            .setTitle("An error occurred, please try again later.")
                            .setPositiveButton("Try again") { dialog, _ ->
                                dialog.dismiss()
                            }.setOnDismissListener {
                                checkAndUpdateLiveData(context)
                            }.setNegativeButton("Exit") { dialog, _ ->
                                dialog.cancel()
                            }.setOnCancelListener {
                                finish()
                            }.show()
                    }

                    else -> {
                        viewModel.updateLiveData(this, binding.swipeRefreshLayout, it)
                    }
                }
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return true
            else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return true
            else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return true
        }
        return false
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(callback: (String?) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) callback.invoke("${it.latitude},${it.longitude}")
            else callback.invoke(null)
        }
    }

    private fun requestLocationPermission(context: Context, activity: Activity) {
        if (!isPermissionGranted() && shouldShowPermissionRationale()) {
            AlertDialog
                .Builder(context)
                .setTitle("Permission Required")
                .setMessage("This app requires permission to display weather data for your current location. If you do not grant this permission, you will not be able to use this application.")
                .setPositiveButton("Continue") { dialog, _ ->
                    dialog.dismiss()
                }.setOnDismissListener {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        REQUEST_LOCATION_PERMISSION_CODE
                    )
                }.setNegativeButton("Exit") { dialog, _ ->
                    dialog.cancel()
                }.setOnCancelListener {
                    finish()
                }.show()
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_LOCATION_PERMISSION_CODE
            && !(grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED))
            && !shouldShowPermissionRationale()
        ) {
            AlertDialog
                .Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app requires permission to display weather data for your current location. If you do not grant this permission, you will not be able to use this application. Please provide the permission in the app settings.")
                .setPositiveButton("Go to settings") { dialog, _ ->
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(
                            Uri.fromParts("package", packageName, null)
                        )
                    )
                    dialog.dismiss()
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(
                        this,
                        "Swipe to refresh after allowing permission and return.",
                        Toast.LENGTH_LONG
                    ).show()
                }.setNegativeButton("Exit") { dialog, _ ->
                    dialog.cancel()
                }.setOnCancelListener {
                    finish()
                }.show()
        } else {
            checkAndUpdateLiveData(this)
        }
    }

    private fun isPermissionGranted(): Boolean =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun shouldShowPermissionRationale(): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
}