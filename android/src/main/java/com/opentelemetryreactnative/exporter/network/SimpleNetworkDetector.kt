package com.opentelemetryreactnative.exporter.network

import android.Manifest
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.annotation.RequiresPermission

class SimpleNetworkDetector(
  private val connectivityManager: ConnectivityManager
) : NetworkDetector {

  @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
  override fun detectCurrentNetwork(): CurrentNetwork {
    val activeNetwork = connectivityManager.activeNetworkInfo
      ?: return NO_NETWORK // Deprecated in API 29

    return when (activeNetwork.type) {
      ConnectivityManager.TYPE_MOBILE -> // Deprecated in API 28
        CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
          .subType(activeNetwork.subtypeName)
          .build()

      ConnectivityManager.TYPE_WIFI -> // Deprecated in API 28
        CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI)
          .subType(activeNetwork.subtypeName)
          .build()

      ConnectivityManager.TYPE_VPN ->
        CurrentNetwork.builder(NetworkState.TRANSPORT_VPN)
          .subType(activeNetwork.subtypeName)
          .build()

      else -> {
        // there is an active network, but it doesn't fall into the neat buckets above
        UNKNOWN_NETWORK
      }
    }
  }

  companion object {
    // These constants are probably imported from elsewhere in the original code
    private val NO_NETWORK = CurrentNetworkProvider.NO_NETWORK
    private val UNKNOWN_NETWORK = CurrentNetworkProvider.UNKNOWN_NETWORK
  }
}
