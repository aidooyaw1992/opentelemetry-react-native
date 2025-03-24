package com.opentelemetryreactnative.exporter.network

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager

interface NetworkDetector {
  fun detectCurrentNetwork(): CurrentNetwork

  companion object {
    fun create(context: Context): NetworkDetector {
      val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val telephonyManager =
          context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val carrierFinder = CarrierFinder(telephonyManager)
        PostApi28NetworkDetector(
          connectivityManager, telephonyManager, carrierFinder, context
        )
      } else {
        SimpleNetworkDetector(connectivityManager)
      }
    }
  }
}
