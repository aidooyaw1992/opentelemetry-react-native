package com.opentelemetryreactnative

import com.facebook.react.bridge.ReadableMap

class ConfigMapReader(private val map:ReadableMap):MapReader() {
  fun getBeaconEndpoint(): String? = Keys.BEACON_ENDPOINT.get(map)
  fun getRumAccessToken(): String? = Keys.TOKEN.get(map)
  fun getGlobalAttributes(): ReadableMap? = Keys.GLOBAL_ATTRIBUTES.getMap(map)
  fun getResource(): ReadableMap? = Keys.RESOURCE.getMap(map)

  private object Keys {
    val BEACON_ENDPOINT = StringKey("beaconEndpoint")
    val TOKEN = StringKey("token")
    val GLOBAL_ATTRIBUTES = MapKey("globalAttributes")
    val RESOURCE = MapKey("resource")
  }

}
