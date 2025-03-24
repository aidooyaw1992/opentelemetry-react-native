package com.opentelemetryreactnative.crash

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeKey.doubleKey
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey

// copied from splunk-otel-android SplunkRum class
class CrashReporterAttributes {
    companion object {
        val SPLUNK_OPERATION_KEY: AttributeKey<String> = stringKey("_splunk_operation")
        val STORAGE_SPACE_FREE_KEY: AttributeKey<Long> = longKey("storage.free")
        val HEAP_FREE_KEY: AttributeKey<Long> = longKey("heap.free")
        val BATTERY_PERCENT_KEY: AttributeKey<Double> = doubleKey("battery.percent")
        val COMPONENT_KEY: AttributeKey<String> = stringKey("component")
        const val COMPONENT_CRASH: String = "crash"
        const val COMPONENT_ERROR: String = "error"
    }
}
