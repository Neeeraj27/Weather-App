package com.example.weather

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.weather.Utilities.ApiUtilitites
import com.example.weather.databinding.ActivityMainBinding
import com.example.weather.models.CurrentWeather
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var currentLocation: Location
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 101
    private val apiKey = "ceaf3d6551f195dccfc074e654cf5d69"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshWeatherData()
        }

        binding.CitySearch.setOnEditorActionListener { textView, i, keyEvent ->
            if (i== EditorInfo.IME_ACTION_SEARCH){

                getCityWeather(binding.CitySearch.text.toString())
                val view=this.currentFocus
                if (view!=null){

                    val imm: InputMethodManager =getSystemService(INPUT_METHOD_SERVICE)
                            as InputMethodManager

                    imm.hideSoftInputFromWindow(view.windowToken,0)

                    binding.CitySearch.clearFocus()
                }
                return@setOnEditorActionListener true
            }
            else{

                return@setOnEditorActionListener false
            }

        }
        binding.mylocation.setOnClickListener {
            getCurrentLocation()
        }
    }
    private fun getCityWeather(city: String) {
        binding.progressbar.visibility=View.VISIBLE
        ApiUtilitites.getApiInterface()?.getCityWeatherData(city,apiKey)?.enqueue(object : Callback<CurrentWeather?> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(
                call: Call<CurrentWeather?>,
                response: Response<CurrentWeather?>
            ) {
                if(response.isSuccessful){
                    binding.progressbar.visibility=View.GONE
                    response.body()?.let {
                        setData(it)
                    }
                }
                else{
                    Toast.makeText(this@MainActivity, "No City Found",
                        Toast.LENGTH_SHORT).show()
                    binding.progressbar.visibility= View.GONE
                }
            }
            override fun onFailure(call: Call<CurrentWeather?>, t: Throwable) {

            }
        })
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager:LocationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    return
                }
                fusedLocationProvider.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocation = location
                        binding.progressbar.visibility = View.VISIBLE

                        fetchCurrentLocationWeather(
                            location.latitude.toString(),
                            location.longitude.toString()
                        )
                    }

                }
            } else {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermission()
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
        arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiUtilitites.getApiInterface()?.getCurrentWeatherData(latitude, longitude, apiKey)?.execute()
                }

                if (response?.isSuccessful == true) {
                    response.body()?.let {
                        setData(it)
                    }
                }
            } catch (e: Exception) {
            } finally {
                binding.progressbar.visibility = View.GONE
            }
        }
    }


    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            return true
        }
        return false
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                // Handle permission denied case
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun setData(body:CurrentWeather) {
        binding.apply {
            tvmintemp.text = "Min temp:"+k2c(body.main.temp_min.toInt())+"°C"
            tvmaxtemp.text = "Max temp:"+k2c(body.main.temp_max.toInt())+"°C"
            tvtemperature.text = ""+k2c(body.main.temp.toInt())+"°C"
            tvwind.text = "${body.wind.speed.toString()} KM/H"
            tvlocation.text = "${body.name}"
            tvpressure.text = "${body.main.pressure} hPa"
            tvsunrisetime.text=ts2td(body.sys.sunrise.toLong())
            val formattedDate = formatDate(body.dt.toLong())
            tvdateandday.text = formattedDate
            tvsunsettime.text=ts2td(body.sys.sunset.toLong())
            tvhumidity.text = "${body.main.humidity}%"
            tvweatherstatus.text = body.weather[0].description
            tvVisibility.text = "${body.visibility}"
            progressbar.visibility = View.GONE
            swipeRefreshLayout.isRefreshing=false
        }
        updateUI(body.weather[0].id)
    }

    private fun updateUI(id: Int) {
        binding.apply {
            when(id){
                //thunderstorm
                in 200.. 232 ->{
                    weatherimage.setImageResource(R.drawable.thunderstorm)
                }
                //Drizzle
                in 300..321 -> {
                    weatherimage.setImageResource(R.drawable.drizzle)
                }
                //Rain
                in 500..531 -> {
                    weatherimage.setImageResource(R.drawable.raiinnnn)
                }
                //Snow
                in 600..622 -> {
                    weatherimage.setImageResource(R.drawable.snow)
                }
                //Atmosphere
                in 701..781 -> {
                    weatherimage.setImageResource(R.drawable.brokenclouds)
                }
                //Clear
                800 -> {
                    weatherimage.setImageResource(R.drawable.cloudandsun
                    )
                }
                //Clouds
                in 801..804 -> {
                    weatherimage.setImageResource(R.drawable.clouds)
                }
                //unknown
                else->{
                    weatherimage.setImageResource(R.drawable.cloudandsun)
                }
            }
        }
    }
    private fun formatDate(date: Long): String {
        val currentDate = Date(date * 1000)
        val dayDateFormat = SimpleDateFormat("EEE, dd MMM", Locale.ENGLISH)
        return dayDateFormat.format(currentDate)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun ts2td(ts: Long): String {
        val localTime=ts.let {
            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()

        }
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
        return localTime.format(formatter)
    }
    private fun k2c(t: Int): Any? {
        var intTemp=t
        intTemp=intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toInt()

    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun refreshWeatherData(){
        if(binding.CitySearch.text.isNotEmpty()){
            getCityWeather(binding.CitySearch.text.toString())
        }else{
            getCurrentLocation()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        getCurrentLocation()
    }



}


