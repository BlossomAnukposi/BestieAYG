package com.bayg

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.chip.Chip
import kotlinx.coroutines.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.*

/**
 * TouchGrassActivity
 *
 * Shows nearby parks fetched from Overpass API (OpenStreetMap data),
 * a weather nudge from Open-Meteo, and an optional osmdroid map.
 *
 * Required in AndroidManifest.xml:
 *   <activity android:name=".TouchGrassActivity" />
 *   <uses-permission android:name="android.permission.INTERNET" />
 *   <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
 *   <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
 *
 * Required build.gradle deps:
 *   implementation 'org.osmdroid:osmdroid-android:6.1.18'
 *   implementation 'com.google.android.gms:play-services-location:21.2.0'
 *   implementation 'com.google.android.material:material:1.11.0'
 */
class TouchGrassActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapView: MapView
    private lateinit var rvParks: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvWeatherLocation: TextView
    private lateinit var tvWeatherNudge: TextView
    private lateinit var layoutEmptyState: View

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Overpass API — fetches parks within 1500m radius
    // Returns node/way/relation with leisure=park
    private val OVERPASS_URL = "https://overpass-api.de/api/interpreter"
    private val SEARCH_RADIUS_METERS = 1500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Required for osmdroid tile caching
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))

        setContentView(R.layout.activity_touch_grass)

        // Toolbar back navigation
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // View refs
        mapView = findViewById(R.id.map_view)
        rvParks = findViewById(R.id.rv_parks)
        pbLoading = findViewById(R.id.pb_parks_loading)
        tvWeatherLocation = findViewById(R.id.tv_weather_location)
        tvWeatherNudge = findViewById(R.id.tv_weather_nudge)
        layoutEmptyState = findViewById(R.id.layout_empty_state)

        // RecyclerView setup
        rvParks.layoutManager = LinearLayoutManager(this)
        rvParks.isNestedScrollingEnabled = false

        // Map setup
        setupMap()

        // Get location then load data
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fetchLocationAndLoad()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
    }

    private fun fetchLocationAndLoad() {
        // Request location permission before this call (use ActivityResultLauncher in production)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    mapView.controller.setCenter(GeoPoint(lat, lon))
                    scope.launch {
                        fetchParks(lat, lon)
                        fetchWeather(lat, lon)
                    }
                } else {
                    showEmptyState()
                }
            }
        } catch (e: SecurityException) {
            showEmptyState()
        }
    }

    // ── Parks from Overpass API ──────────────────────────────────────────────

    private suspend fun fetchParks(lat: Double, lon: Double) = withContext(Dispatchers.IO) {
        val query = """
            [out:json][timeout:25];
            (
              node[leisure=park](around:$SEARCH_RADIUS_METERS,$lat,$lon);
              way[leisure=park](around:$SEARCH_RADIUS_METERS,$lat,$lon);
              relation[leisure=park](around:$SEARCH_RADIUS_METERS,$lat,$lon);
            );
            out center;
        """.trimIndent()

        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val response = URL("$OVERPASS_URL?data=$encoded").readText()
            val json = JSONObject(response)
            val elements = json.getJSONArray("elements")

            val parks = mutableListOf<Park>()
            for (i in 0 until elements.length()) {
                val el = elements.getJSONObject(i)
                val tags = el.optJSONObject("tags") ?: continue
                val name = tags.optString("name", "").ifBlank { continue }

                // Coordinates: node has lat/lon directly; way/relation have a center object
                val parkLat: Double
                val parkLon: Double
                if (el.getString("type") == "node") {
                    parkLat = el.getDouble("lat")
                    parkLon = el.getDouble("lon")
                } else {
                    val center = el.optJSONObject("center") ?: continue
                    parkLat = center.getDouble("lat")
                    parkLon = center.getDouble("lon")
                }

                val distanceM = haversineMeters(lat, lon, parkLat, parkLon)
                parks.add(Park(name, parkLat, parkLon, distanceM))
            }

            parks.sortBy { it.distanceMeters }

            withContext(Dispatchers.Main) {
                pbLoading.visibility = View.GONE
                if (parks.isEmpty()) {
                    showEmptyState()
                } else {
                    layoutEmptyState.visibility = View.GONE
                    rvParks.visibility = View.VISIBLE
                    rvParks.adapter = ParkAdapter(parks) { park -> openInMaps(park) }
                    addMapMarkers(parks)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                pbLoading.visibility = View.GONE
                showEmptyState()
            }
        }
    }

    private fun addMapMarkers(parks: List<Park>) {
        parks.forEach { park ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(park.lat, park.lon)
            marker.title = park.name
            marker.snippet = park.distanceLabel
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    private fun openInMaps(park: Park) {
        val uri = Uri.parse("geo:${park.lat},${park.lon}?q=${Uri.encode(park.name)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback: open in browser maps
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://maps.google.com/?q=${Uri.encode(park.name)}&ll=${park.lat},${park.lon}")))
        }
    }

    // ── Weather from Open-Meteo ──────────────────────────────────────────────
    // Open-Meteo is free, no API key needed.

    private suspend fun fetchWeather(lat: Double, lon: Double) = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current_weather=true" +
                    "&hourly=apparent_temperature"
            val json = JSONObject(URL(url).readText())
            val current = json.getJSONObject("current_weather")
            val tempC = current.getDouble("temperature").toInt()
            val weatherCode = current.getInt("weathercode")
            val description = weatherCodeToDescription(weatherCode)

            // Reverse geocode city name from coordinates using Nominatim (OSM)
            val nominatimUrl = "https://nominatim.openstreetmap.org/reverse" +
                    "?lat=$lat&lon=$lon&format=json&zoom=10"
            val nominatimJson = JSONObject(
                URL(nominatimUrl).openConnection().also {
                    (it as HttpURLConnection).setRequestProperty(
                        "User-Agent", "BestieAreYouGood/1.0")
                }.getInputStream().bufferedReader().readText()
            )
            val address = nominatimJson.optJSONObject("address")
            val city = address?.optString("city")
                ?: address?.optString("town")
                ?: address?.optString("village")
                ?: "Your location"

            val nudge = weatherNudge(tempC, weatherCode)

            withContext(Dispatchers.Main) {
                tvWeatherLocation.text = "$city · ${tempC}°C · $description"
                tvWeatherNudge.text = nudge
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                tvWeatherLocation.text = "Weather unavailable"
                tvWeatherNudge.text = "Still a good day to get outside!"
            }
        }
    }

    private fun weatherCodeToDescription(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1, 2, 3 -> "Partly Cloudy"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rainy"
        71, 73, 75 -> "Snowy"
        80, 81, 82 -> "Showers"
        95 -> "Thunderstorm"
        else -> "Cloudy"
    }

    private fun weatherNudge(tempC: Int, weatherCode: Int): String {
        return when {
            weatherCode in 61..82 -> "It's a little wet — but puddle-jumping counts."
            weatherCode in 95..99 -> "Okay, maybe wait for the thunder to pass…"
            tempC > 28 -> "It's ${tempC}°C — stay hydrated and touch grass briefly."
            tempC in 15..28 -> "It's ${tempC}°C — perfect touch-grass weather, no excuses."
            tempC in 5..14 -> "It's ${tempC}°C — fresh air is good for the soul. Jacket up."
            else -> "It's cold, but a short walk still counts."
        }
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth radius in meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lon2 - lon1)
        val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun showEmptyState() {
        pbLoading.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
        rvParks.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

// ── Data model ───────────────────────────────────────────────────────────────

data class Park(
    val name: String,
    val lat: Double,
    val lon: Double,
    val distanceMeters: Double
) {
    val distanceLabel: String
        get() {
            val km = distanceMeters / 1000.0
            val walkMinutes = (distanceMeters / 80).toInt()   // ~80m/min walking
            val bikeMinutes = (distanceMeters / 250).toInt()  // ~250m/min cycling
            return if (distanceMeters < 1000) {
                "${distanceMeters.toInt()} m · ~$walkMinutes min walk"
            } else {
                "${"%.1f".format(km)} km · ~$walkMinutes min walk"
            }
        }
}

// ── RecyclerView Adapter ──────────────────────────────────────────────────────

class ParkAdapter(
    private val parks: List<Park>,
    private val onOpenMaps: (Park) -> Unit
) : RecyclerView.Adapter<ParkAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_park_name)
        val tvDistance: TextView = view.findViewById(R.id.tv_park_distance)
        val chipClosest: Chip = view.findViewById(R.id.chip_closest)
        val btnMaps: ImageButton = view.findViewById(R.id.btn_open_maps)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_park, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val park = parks[position]
        holder.tvName.text = park.name
        holder.tvDistance.text = park.distanceLabel
        holder.chipClosest.visibility = if (position == 0) View.VISIBLE else View.GONE
        holder.btnMaps.setOnClickListener { onOpenMaps(park) }
    }

    override fun getItemCount() = parks.size
}