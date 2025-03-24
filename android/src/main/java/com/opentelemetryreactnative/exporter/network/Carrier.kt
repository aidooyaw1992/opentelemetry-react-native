package com.opentelemetryreactnative.exporter.network

import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import java.util.Objects

@RequiresApi(api = Build.VERSION_CODES.P)
class Carrier private constructor(builder: Builder) {
  val id: Int = builder.id
  val name: String? = builder.name
  val mobileCountryCode: String? = builder.mobileCountryCode // 3 digits
  val mobileNetworkCode: String? = builder.mobileNetworkCode // 2 or 3 digits
  val isoCountryCode: String? = builder.isoCountryCode

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val carrier = other as Carrier
    return id == carrier.id &&
      Objects.equals(name, carrier.name) &&
      Objects.equals(mobileCountryCode, carrier.mobileCountryCode) &&
      Objects.equals(mobileNetworkCode, carrier.mobileNetworkCode) &&
      Objects.equals(isoCountryCode, carrier.isoCountryCode)
  }

  override fun hashCode(): Int {
    return Objects.hash(id, name, mobileCountryCode, mobileNetworkCode, isoCountryCode)
  }

  override fun toString(): String {
    return "Carrier{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", mobileCountryCode='" + mobileCountryCode + '\'' +
      ", mobileNetworkCode='" + mobileNetworkCode + '\'' +
      ", isoCountryCode='" + isoCountryCode + '\'' +
      '}'
  }

  class Builder {
    var id: Int = TelephonyManager.UNKNOWN_CARRIER_ID
      private set
    var name: String? = null
      private set
    var mobileCountryCode: String? = null
      private set
    var mobileNetworkCode: String? = null
      private set
    var isoCountryCode: String? = null
      private set

    fun build(): Carrier {
      return Carrier(this)
    }

    fun id(id: Int): Builder {
      this.id = id
      return this
    }

    fun name(name: String): Builder {
      this.name = name
      return this
    }

    fun mobileCountryCode(countryCode: String): Builder {
      this.mobileCountryCode = countryCode
      return this
    }

    fun mobileNetworkCode(networkCode: String): Builder {
      this.mobileNetworkCode = networkCode
      return this
    }

    fun isoCountryCode(isoCountryCode: String): Builder {
      this.isoCountryCode = isoCountryCode
      return this
    }
  }

  companion object {
    fun builder(): Builder {
      return Builder()
    }
  }
}
