package com.opentelemetryreactnative.exporter.network

import io.opentelemetry.semconv.trace.attributes.SemanticAttributes

enum class NetworkState(val humanName: String) {
  NO_NETWORK_AVAILABLE(SemanticAttributes.NetHostConnectionTypeValues.UNAVAILABLE),
  TRANSPORT_CELLULAR(SemanticAttributes.NetHostConnectionTypeValues.CELL),
  TRANSPORT_WIFI(SemanticAttributes.NetHostConnectionTypeValues.WIFI),
  TRANSPORT_UNKNOWN(SemanticAttributes.NetHostConnectionTypeValues.UNKNOWN),
  // this one doesn't seem to have an otel value at this point.
  TRANSPORT_VPN("vpn")
}
