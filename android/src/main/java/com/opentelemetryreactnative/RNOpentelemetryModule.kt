package com.opentelemetryreactnative

import android.app.Application
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.opentelemetryreactnative.crash.CrashEventAttributeExtractor
import com.opentelemetryreactnative.crash.CrashReporter
import com.opentelemetryreactnative.exporter.network.CurrentNetworkAttributesExtractor
import com.opentelemetryreactnative.exporter.network.CurrentNetworkProvider
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import io.opentelemetry.sdk.trace.export.SpanExporter
import kotlin.concurrent.Volatile

class RNOpentelemetryModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private val moduleStartTime: Long = System.currentTimeMillis()

  @Volatile
  private var crashReporter: CrashReporter? = null

  @Volatile
  private var exporter: SpanExporter? = null

  private var currentNetworkProvider: CurrentNetworkProvider? = null

  override fun getName(): String = NAME

  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  @ReactMethod
  fun multiply(a: Double, b: Double, promise: Promise) {
    promise.resolve(a * b)
  }

  @ReactMethod
  fun initializeRUM(configMap: ReadableMap, promise: Promise) {
    try {
      val mapReader = ConfigMapReader(configMap)
      val beaconEndpoint = mapReader.getBeaconEndpoint()

      if (beaconEndpoint == null) {
        reportFailure(promise, "Initialize: cannot construct exporter, endpoint missing");
        return
      }
      currentNetworkProvider =
        CurrentNetworkProvider.createAndStart(reactApplicationContext.applicationContext as Application)
      exporter = createExporter(beaconEndpoint)

      crashReporter = CrashReporter(
        exporter = exporter!!,
        currentNetworkProvider = currentNetworkProvider!!,
        globalAttributes = attributesFromMap(mapReader.getGlobalAttributes()),
        context = reactApplicationContext.applicationContext
      )

      crashReporter!!.install()

      val appStartInfo: WritableMap = Arguments.createMap()
      val appStart = PerfProvider.getAppStartTime()
      val appStartTracker: AppStartTracker = AppStartTracker.instance
      appStartInfo.putDouble("appStart", appStart.toDouble())
      appStartInfo.putDouble("moduleStart", this.moduleStartTime.toDouble())
      appStartInfo.putBoolean("isColdStart", appStartTracker.isColdStart)
      promise.resolve(appStartInfo)

    } catch (e: Exception) {
      Log.e(LogConstants.LOG_TAG, "initializeRUM: Error", e)
      promise.reject(e)
    }
  }

  @ReactMethod
  fun setGlobalAttributes(attributesMap: ReadableMap) {
    val attributesFromMap = attributesFromMap(attributesMap)
    setGlobalAttributes(attributesFromMap)
  }

  private fun setGlobalAttributes(attributes: Attributes) {
    val currentCrashReporter = crashReporter
    if (currentCrashReporter == null) {
      Log.e(LogConstants.LOG_TAG, "setGlobalAttributes: crash reporter not initialized")
      return
    }
    currentCrashReporter.updateGlobalAttributes(attributes)
  }

  @ReactMethod
  fun setSessionId(sessionId: String?) {
    crashReporter?.let {
      val currentCrashReporter: CrashReporter = crashReporter!!
      currentCrashReporter.updateSessionId(sessionId)
      Log.d(LogConstants.LOG_TAG, "setSessionId: ")
    }
  }

  @ReactMethod
  fun export(spanMaps: ReadableArray, promise: Promise) {
    try {
      val currentExporter = exporter
      if (currentExporter == null) {
        reportFailure(promise, "Export: exporter not initialized")
        return
      }

      val spanDataList = mutableListOf<SpanData>()
      val network = currentNetworkProvider?.refreshNetworkStatus()
      val networkAttributesExtractor = CurrentNetworkAttributesExtractor()
      val networkAttributes = networkAttributesExtractor.extract(network!!)

      for (i in 0 until spanMaps.size()) {
        val spanMap = spanMaps.getMap(i)
        Log.d(LogConstants.LOG_TAG, "Processing span $i: $spanMap")

        val mapReader = SpanMapReader(spanMap!!)
        val context = contextFromMap(mapReader)

        if (!context.isValid) {
          Log.e(LogConstants.LOG_TAG, "Export: trace or span ID not provided")
          reportFailure(promise, "Export: trace or span ID not provided")
          return
        }

        Log.d(LogConstants.LOG_TAG, "Valid context created with traceId: ${context.traceId}")

        val spanProperties = propertiesFromMap(mapReader) ?: run {
          Log.e(LogConstants.LOG_TAG, "Export: missing name, start or end time")
          reportFailure(promise, "Export: missing name, start or end time")
          return
        }

        Log.d(LogConstants.LOG_TAG, "Span properties created: ${spanProperties.name}")
        val parentContext = parentContextFromMap(mapReader, context)

        val attributes =
          attributesFromMap(mapReader.attributes).toBuilder().putAll(networkAttributes).build()
        val spanData = ReactSpanData(
          spanProperties,
          attributes,
          context,
          parentContext,
          emptyList()
        )

        spanDataList.add(spanData)
        Log.d(LogConstants.LOG_TAG, "Created span with attributes: $attributes")
      }
      currentExporter.export(spanDataList)
      Log.d(LogConstants.LOG_TAG, "Export completed successfully")
      promise.resolve(null)

    } catch (e: Exception) {
      Log.e(LogConstants.LOG_TAG, "Export failed", e)
      promise.reject("EXPORT_ERROR", e)
    }
  }

  @ReactMethod
  fun nativeCrash() {
    Thread {
      try {
        Thread.sleep(2000)
      } catch (e: InterruptedException) {
        // Empty catch block maintained from original
      }
      throw RuntimeException("test crash")
    }.start()
  }

  private fun createExporter(endpoint: String): SpanExporter {
    return CrashEventAttributeExtractor(
      OtlpHttpSpanExporter.builder()
        .setEndpoint(endpoint).build()
    )
  }

  private fun propertiesFromMap(mapReader: SpanMapReader): ReactSpanProperties? {
    val name = mapReader.name
    val startEpochMillis = mapReader.startEpochMillis
    val endEpochMillis = mapReader.endEpochMillis
    val tracerName = mapReader.tracerName
    if (tracerName == null || name == null || startEpochMillis == null || endEpochMillis == null) {
      return null
    }

    return ReactSpanProperties(
      name = name,
      kind = SpanKind.INTERNAL,
      status = StatusData.ok(),
      startTimeNanos = millisToNanos(startEpochMillis),
      endTimeNanos = millisToNanos(endEpochMillis)
    )
  }

  private fun parentContextFromMap(
    mapReader: SpanMapReader,
    childContext: SpanContext
  ): SpanContext {
    val parentSpanId = mapReader.parentSpanId ?: return SpanContext.getInvalid()

    return SpanContext.create(
      childContext.traceId,
      parentSpanId,
      childContext.traceFlags,
      TraceState.getDefault()
    )
  }

  private fun attributesFromMap(attributeMap: ReadableMap?): Attributes {
    if (attributeMap == null) return Attributes.empty()

    val builder = Attributes.builder()
    val iterator = attributeMap.entryIterator

    while (iterator.hasNext()) {
      val entry = iterator.next()
      when (val value = entry.value) {
        is String -> builder.put(entry.key, value)
        is Number -> {
          // TODO fix this
          if (entry.key == "http.status_code") {
            builder.put(entry.key, value.toString())
          } else {
            builder.put(entry.key, value.toDouble())
          }
        }
      }
    }
    return builder.build()
  }

  private fun contextFromMap(mapReader: SpanMapReader): SpanContext {
    val traceId = mapReader.traceId
    val spanId = mapReader.spanId
    val traceFlagsNumeric = mapReader.traceFlags

    if (traceId == null || spanId == null) {
      return SpanContext.getInvalid()
    }

    val traceFlags = traceFlagsNumeric?.let {
      TraceFlags.fromByte(it.toByte())
    } ?: TraceFlags.getSampled()

    return SpanContext.create(
      traceId,
      spanId,
      traceFlags,
      TraceState.getDefault()
    )
  }

  companion object {
    const val NAME = "RNOpentelemetry"

    fun reportFailure(promise: Promise, message: String) {
      promise.reject("OTEL_ERROR", message)
    }

    fun millisToNanos(millis: Long) = millis * 1000000
  }
}
