package com.bayg.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayg.Park
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder

class NearestParkViewModel : ViewModel() {

    private val _parkState = MutableStateFlow<NearestParkUiState>(NearestParkUiState.Loading)
    val parkState: StateFlow<NearestParkUiState> = _parkState

    private val overpassUrl = "https://overpass-api.de/api/interpreter"
    private val searchRadiusMeters = 1500

    fun fetchNearestPark(lat: Double, lon: Double) {
        viewModelScope.launch {
            _parkState.value = NearestParkUiState.Loading
            try {
                val (nearest, total) = fetchFromOverpass(lat, lon)
                    ?: run {
                        _parkState.value = NearestParkUiState.Error("No parks found nearby")
                        return@launch
                    }
                _parkState.value = NearestParkUiState.Success(nearest, total)
            } catch (e: Exception) {
                _parkState.value = NearestParkUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun fetchFromOverpass(lat: Double, lon: Double): Pair<Park, Int>? =
        withContext(Dispatchers.IO) {
            val query = """
            [out:json][timeout:25];
            (
              node[leisure=park](around:$searchRadiusMeters,$lat,$lon);
              way[leisure=park](around:$searchRadiusMeters,$lat,$lon);
              relation[leisure=park](around:$searchRadiusMeters,$lat,$lon);
            );
            out center;
        """.trimIndent()

            val encoded = URLEncoder.encode(query, "UTF-8")
            val response = java.net.URL("$overpassUrl?data=$encoded").readText()
            val elements = JSONObject(response).getJSONArray("elements")

            val parks = (0 until elements.length()).mapNotNull { i ->
                val el = elements.getJSONObject(i)
                val tags = el.optJSONObject("tags") ?: return@mapNotNull null
                val name = tags.optString("name", "").takeIf { it.isNotBlank() } ?: return@mapNotNull null

                val parkLat: Double
                val parkLon: Double
                if (el.getString("type") == "node") {
                    parkLat = el.getDouble("lat")
                    parkLon = el.getDouble("lon")
                } else {
                    val center = el.optJSONObject("center") ?: return@mapNotNull null
                    parkLat = center.getDouble("lat")
                    parkLon = center.getDouble("lon")
                }

                Park(name, parkLat, parkLon, haversine(lat, lon, parkLat, parkLon))
            }

            val nearest = parks.minByOrNull { it.distanceMeters } ?: return@withContext null
            Pair(nearest, parks.size)
        }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1); val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dPhi / 2).pow(2) + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2).pow(2)
        return 6_371_000.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}

private fun Double.pow(n: Int): Double = Math.pow(this, n.toDouble())