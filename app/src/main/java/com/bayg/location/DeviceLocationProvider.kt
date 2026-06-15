package com.bayg.location

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

object DeviceLocationProvider {

    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancellation = CancellationTokenSource()

        val location = try {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token).await()
        } catch (_: SecurityException) {
            null
        } ?: try {
            client.lastLocation.await()
        } catch (_: SecurityException) {
            null
        }

        return location?.let { it.latitude to it.longitude }
    }
}
