package com.opentelemetryreactnative.exporter.network

import io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_ICC
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_MCC
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_MNC
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_NAME
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_SUBTYPE
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_TYPE
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder

class CurrentNetworkAttributesExtractor {
  fun extract(network: CurrentNetwork): Attributes {
    val builder = Attributes.builder()
      .put(NET_HOST_CONNECTION_TYPE, network.state.humanName)

    setIfNotNull(builder, NET_HOST_CONNECTION_SUBTYPE, network.getSubType())
    setIfNotNull(builder, NET_HOST_CARRIER_NAME, network.getCarrierName())
    setIfNotNull(builder, NET_HOST_CARRIER_MCC, network.getCarrierCountryCode())
    setIfNotNull(builder, NET_HOST_CARRIER_MNC, network.getCarrierNetworkCode())
    setIfNotNull(builder, NET_HOST_CARRIER_ICC, network.getCarrierIsoCountryCode())

    return builder.build()
  }

  private companion object {
    private fun setIfNotNull(
      builder: AttributesBuilder,
      key: AttributeKey<String>,
      value: String?
    ) {
      value?.let { builder.put(key, it) }
    }
  }
}
