package com.opentelemetryreactnative.exporter.network

import android.os.Build;
import android.telephony.TelephonyManager;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.P)
class CarrierFinder(private val telephonyManager: TelephonyManager) {

  fun get(): Carrier {
    val builder = Carrier.builder()
    val id = telephonyManager.simCarrierId
    builder.id(id)

    val name = telephonyManager.simCarrierIdName
    if (validString(name)) {
      builder.name(name.toString())
    }

    val simOperator = telephonyManager.simOperator
    if (validString(simOperator) && simOperator.length >= 5) {
      val countryCode = simOperator.substring(0, 3)
      val networkCode = simOperator.substring(3)
      builder.mobileCountryCode(countryCode).mobileNetworkCode(networkCode)
    }

    val isoCountryCode = telephonyManager.simCountryIso
    if (validString(isoCountryCode)) {
      builder.isoCountryCode(isoCountryCode)
    }

    return builder.build()
  }

  private fun validString(str: CharSequence?): Boolean {
    return !str.isNullOrEmpty()
  }
}
