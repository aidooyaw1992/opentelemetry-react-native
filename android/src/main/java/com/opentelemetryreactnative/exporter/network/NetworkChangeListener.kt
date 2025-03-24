package com.opentelemetryreactnative.exporter.network

interface NetworkChangeListener {
  fun onNetworkChange(currentNetwork: CurrentNetwork?)
}
