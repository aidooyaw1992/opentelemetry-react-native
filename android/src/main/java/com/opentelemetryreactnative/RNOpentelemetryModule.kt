package com.opentelemetryreactnative

import android.os.Build
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.TimeUnit


class RNOpentelemetryModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private val moduleStartTime: Long = System.currentTimeMillis()
  private lateinit var tracerProvider: SdkTracerProvider

  @Volatile
  private var exporter: SpanExporter? = null

  private var tracer: Tracer? = null

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
      val resourceMap = mapReader.getResource()
      val token = mapReader.getRumAccessToken()

      val resource = if (resourceMap == null) {
        Resource.getDefault()
      } else {
        // Build React Native attributes
        val resourceAttributesBuilder = Attributes.builder()
        resourceMap.toHashMap().forEach { (key, value) ->
          when (value) {
            is String -> resourceAttributesBuilder.put(key, value)
            is Number -> resourceAttributesBuilder.put(key, value.toLong())
            is Boolean -> resourceAttributesBuilder.put(key, value)
          }
        }

        // Add device attributes
        resourceAttributesBuilder
          .put("service.name", "mymtn-app")
          .put("device.model", Build.MODEL)
          .put("device.manufacturer", Build.MANUFACTURER)
          .put("package.name", BuildConfig.LIBRARY_PACKAGE_NAME)

        // Merge default with all attributes
        Resource.getDefault()
          .merge(Resource.create(resourceAttributesBuilder.build()))
      }

      if (beaconEndpoint == null) {
        reportFailure(promise, "Initialize: cannot construct exporter, endpoint or token missing");
        return
      }
      // val apiKey = "ApiKey Z0ZLU3VKVUJNeV81cTF4cS00MnQ6SVAwRDhVYUdDT0NzRFVxMG02YjZRUQ=="

        // Log.d(LogConstants.LOG_TAG, "$apiKey")
        exporter = OtlpHttpSpanExporter.builder()
          .setEndpoint(beaconEndpoint)
          // .addHeader("Authorization", apiKey)
          .build()

      // Create TracerProvider
      tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(exporter!!).build())
        .addResource(resource)
        .build()

      tracer = tracerProvider.get("native-tracer")

      val appStartInfo: WritableMap = Arguments.createMap()
      val appStart = PerfProvider.getAppStartTime()
      val appStartTracker: AppStartTracker = AppStartTracker.instance
      appStartInfo.putDouble("appStart", appStart.toDouble())
      appStartInfo.putDouble("moduleStart", this.moduleStartTime.toDouble())
      appStartInfo.putBoolean("isColdStart", appStartTracker.isColdStart)
      promise.resolve(appStartInfo)

    } catch (e: Exception) {
      promise.reject(e)
    }
  }

  @ReactMethod
  fun setSessionId(sessionId: String?) {
//    val currentCrashReporter: CrashReporter = crashReporter
//
//    if (currentCrashReporter != null) {
//      currentCrashReporter.updateSessionId(sessionId)
//    }
  }

  @ReactMethod
  fun export(spanMaps: ReadableArray, promise: Promise) {
    try {

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

        //create spanBuilder
        val tracer = tracerProvider.get(spanProperties.tracerName)
        val spanBuilder = tracer.spanBuilder(spanProperties.name)
          .setSpanKind(spanProperties.kind)


//          .setStartTimestamp(spanProperties.startTimeNanos, TimeUnit.MILLISECONDS)

        val attributes = attributesFromMap(mapReader.attributes)
          .toBuilder()
          .build()

        Log.d(LogConstants.LOG_TAG, "Created span with attributes: $attributes")

        val span = spanBuilder.startSpan()
          .setAllAttributes(attributes)
          .setStatus(spanProperties.status.statusCode)

        span.end()
      }

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

  private fun propertiesFromMap(mapReader: SpanMapReader): ReactSpanProperties? {
    val name = mapReader.name
    val startEpochMillis = mapReader.startEpochMillis
    val endEpochMillis = mapReader.endEpochMillis
    val tracerName = mapReader.tracerName
    if (tracerName == null || name == null || startEpochMillis == null || endEpochMillis == null) {
      return null
    }

    return ReactSpanProperties(
      tracerName = tracerName,
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
