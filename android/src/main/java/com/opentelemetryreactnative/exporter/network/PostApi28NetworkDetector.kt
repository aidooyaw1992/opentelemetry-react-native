package com.opentelemetryreactnative.exporter.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

@RequiresApi(api = Build.VERSION_CODES.P)
class PostApi28NetworkDetector(
  private val connectivityManager: ConnectivityManager,
  private val telephonyManager: TelephonyManager,
  private val carrierFinder: CarrierFinder,
  private val context: Context
) : NetworkDetector {

  @SuppressLint("MissingPermission")
  override fun detectCurrentNetwork(): CurrentNetwork {
    val capabilities =
      connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return NO_NETWORK

    val carrier = carrierFinder.get()

    return when {
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
        // If the app has the permission, use it to get a subtype.
        val subType = if (hasPermission(Manifest.permission.READ_PHONE_STATE)) {
          getDataNetworkTypeName(telephonyManager.dataNetworkType)
        } else {
          null
        }

        CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
          .carrier(carrier)
          .subType(subType)
          .build()
      }
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
        CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI)
          .carrier(carrier)
          .build()
      }
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
        CurrentNetwork.builder(NetworkState.TRANSPORT_VPN)
          .carrier(carrier)
          .build()
      }
      else -> {
        // there is an active network, but it doesn't fall into the neat buckets above
        UNKNOWN_NETWORK
      }
    }
  }

  // visible for testing
  private fun hasPermission(permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(context, permission) ==
      PackageManager.PERMISSION_GRANTED
  }

  private fun getDataNetworkTypeName(dataNetworkType: Int): String {
    return when (dataNetworkType) {
      TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
      TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
      TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
      TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD"
      TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
      TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
      TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
      TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
      TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
      TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
      TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
      TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPAP"
      TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
      TelephonyManager.NETWORK_TYPE_IDEN -> "IDEN"
      TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
      TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
      TelephonyManager.NETWORK_TYPE_NR -> "NR"
      TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "SCDMA"
      TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
      TelephonyManager.NETWORK_TYPE_UNKNOWN -> "UNKNOWN"
      else -> "UNKNOWN"
    }
  }

  companion object {
    // These constants are probably imported from elsewhere in the original code
    private val NO_NETWORK = CurrentNetworkProvider.NO_NETWORK
    private val UNKNOWN_NETWORK = CurrentNetworkProvider.UNKNOWN_NETWORK
  }
}
