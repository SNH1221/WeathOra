package com.skyrist.weathora

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.skyrist.weathora.databinding.ActivityMainBinding
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*



// Your API Key

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Internet Check Added Here 🔽
        if (NetworkUtils.isInternetAvailable(this)) {
            fetchWeatherData("Lucknow")
        } else {
            showRetryDialog()
        }

        SearchCity()
    }

    private fun SearchCity() {
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    if (NetworkUtils.isInternetAvailable(this@MainActivity)) {
                        fetchWeatherData(query)
                    } else {
                        showRetryDialog()
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    // 🔁 Retry Dialog
    private fun showRetryDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("No Internet Connection")
        builder.setMessage("Please check your connection and try again.")
        builder.setPositiveButton("Retry") { dialog, _ ->
            dialog.dismiss()
            if (NetworkUtils.isInternetAvailable(this)) {
                fetchWeatherData("Lucknow") // or last searched city
            } else {
                showRetryDialog()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun fetchWeatherData(cityName: String) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(ApiInterFace::class.java)

        val response = retrofit.getWeatherData(cityName, "API Key", "metric")
        response.enqueue(object : Callback<WeathOra> {
            override fun onResponse(call: Call<WeathOra>, response: Response<WeathOra>) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    val temperature = responseBody.main.temp.toString()
                    val humidity = responseBody.main.humidity
                    val windSpeed = responseBody.wind.speed
                    val sunRise = responseBody.sys.sunrise.toLong()
                    val sunSet = responseBody.sys.sunset.toLong()
                    val seaLevel = responseBody.main.pressure
                    val condition = responseBody.weather.firstOrNull()?.main ?: "unknown"
                    val maxTemp = responseBody.main.temp_max
                    val minTemp = responseBody.main.temp_min

                    binding.temp.text = "$temperature °C"
                    binding.weather.text = condition
                    binding.maxTemp.text = "Max Temp: $maxTemp °C"
                    binding.minTemp.text = "Min Temp: $minTemp °C"
                    binding.humidity.text = "$humidity %"
                    binding.windSpeed.text = "$windSpeed m/s"
                    binding.sunRise.text = "${time(sunRise)}"
                    binding.sunset.text = "${time(sunSet)}"
                    binding.sea.text = "$seaLevel hPa"
                    binding.condition.text = condition
                    binding.cityName.text = "$cityName"
                    binding.day.text = dayName(System.currentTimeMillis())
                    binding.date.text = date()

                    changeBackground(condition)
                }
            }

            override fun onFailure(call: Call<WeathOra>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Failed to fetch weather", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun changeBackground(conditions: String) {
        when (conditions) {
            "Haze", "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy" -> {
                binding.root.setBackgroundResource(R.drawable.cloud_background)
                binding.lottieAnimationView.setAnimation(R.raw.cloud)
            }

            "Clear Sky", "Sunny", "Clear" -> {
                binding.root.setBackgroundResource(R.drawable.sunny_background)
                binding.lottieAnimationView.setAnimation(R.raw.sun)
            }

            "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain", "Rain" -> {
                binding.root.setBackgroundResource(R.drawable.rain_background)
                binding.lottieAnimationView.setAnimation(R.raw.rain)
            }

            "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard", "Snow" -> {
                binding.root.setBackgroundResource(R.drawable.snow_background)
                binding.lottieAnimationView.setAnimation(R.raw.snow)
            }

            else -> {
                binding.root.setBackgroundResource(R.drawable.sunny_background)
                binding.lottieAnimationView.setAnimation(R.raw.sun)
            }
        }

        binding.lottieAnimationView.playAnimation()
    }

    private fun date(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun time(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    fun dayName(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date())
        }
}