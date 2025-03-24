package com.opentelemetryreactnative.exporter.network

import android.os.Build
import java.util.Objects

/** A value class representing the current network that the device is connected to. */
class CurrentNetwork private constructor(builder: Builder) {
  private val carrier: Carrier? = builder.carrier
  val state: NetworkState = builder.state
  private val subType: String? = builder.subType

  /** Returns [true] if the device has internet connection; [false] otherwise. */
  fun isOnline(): Boolean {
    return state != NetworkState.NO_NETWORK_AVAILABLE
  }

  fun getSubType(): String? {
    return subType
  }

  fun getCarrierCountryCode(): String? {
    return if (haveCarrier()) carrier?.mobileCountryCode else null
  }

  fun getCarrierIsoCountryCode(): String? {
    return if (haveCarrier()) carrier?.isoCountryCode else null
  }

  fun getCarrierNetworkCode(): String? {
    return if (haveCarrier()) carrier?.mobileNetworkCode else null
  }

  fun getCarrierName(): String? {
    return if (haveCarrier()) carrier?.name else null
  }

  private fun haveCarrier(): Boolean {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) && (carrier != null)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val that = other as CurrentNetwork
    return Objects.equals(carrier, that.carrier) &&
      state == that.state &&
      Objects.equals(subType, that.subType)
  }

  override fun hashCode(): Int {
    return Objects.hash(carrier, state, subType)
  }

  override fun toString(): String {
    return "CurrentNetwork{" +
      "carrier=" + carrier +
      ", state=" + state +
      ", subType='" + subType + '\'' +
      '}'
  }

  companion object {
    fun builder(state: NetworkState): Builder {
      return Builder(state)
    }
  }

  class Builder internal constructor(val state: NetworkState) {
    var carrier: Carrier? = null
      private set
    var subType: String? = null
      private set

    fun carrier(carrier: Carrier?): Builder {
      this.carrier = carrier
      return this
    }

    fun subType(subType: String?): Builder {
      this.subType = subType
      return this
    }

    fun build(): CurrentNetwork {
      return CurrentNetwork(this)
    }
  }
}
