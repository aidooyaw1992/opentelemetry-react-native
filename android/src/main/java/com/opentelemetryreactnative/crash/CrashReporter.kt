package com.opentelemetryreactnative.crash

import android.content.Context
import com.opentelemetryreactnative.ReactSpanData
import com.opentelemetryreactnative.ReactSpanProperties
import com.opentelemetryreactnative.exporter.network.CurrentNetworkAttributesExtractor
import com.opentelemetryreactnative.exporter.network.CurrentNetworkProvider
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.internal.ImmutableSpanContext
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.common.Clock
import io.opentelemetry.sdk.trace.IdGenerator
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.ExceptionEventData
import io.opentelemetry.sdk.trace.data.StatusData
import io.opentelemetry.sdk.trace.export.SpanExporter
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import java.util.concurrent.atomic.AtomicBoolean

class CrashReporter(
  private val exporter: SpanExporter,
  private val currentNetworkProvider: CurrentNetworkProvider,
  @Volatile private var globalAttributes: Attributes,
  context: Context
) {

  @Volatile
  private var sessionId: String? = null

  private val runtimeDetailsExtractor: RuntimeDetailsExtractor =
    RuntimeDetailsExtractor.create(context)
  private val crashHappened = AtomicBoolean(false)
  private val clock: AnchoredClock = AnchoredClock.create(Clock.getDefault())
  private val idGenerator: IdGenerator = IdGenerator.random()


  fun updateSessionId(sessionId: String?) {
    this.sessionId = sessionId
  }

  fun updateGlobalAttributes(globalAttributes: Attributes?) {
    this.globalAttributes = globalAttributes!!
  }

  fun install() {
    val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      exportCrashSpan(thread, throwable)
      exporter.flush()
      existingHandler?.uncaughtException(thread, throwable)
    }
  }

  private fun exportCrashSpan(thread: Thread, exception: Throwable) {
    val epochNanos = clock.now()
    val properties = buildProperties(exception, epochNanos)
    val spanData = ReactSpanData(
      properties,
      buildAttributes(properties, thread),
      buildContext(),
      SpanContext.getInvalid(),
      buildEvents(exception, epochNanos, buildAttributes(properties, thread))
    )
    exporter.export(listOf(spanData))
  }

  private fun buildContext(): SpanContext {
    val spanId = idGenerator.generateSpanId()
    val traceId = idGenerator.generateTraceId()
    return ImmutableSpanContext.create(
      traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault(), false, true
    )
  }

  private fun buildAttributes(properties: ReactSpanProperties, thread: Thread): Attributes {
    val attributes = Attributes.builder().putAll(globalAttributes)

    attributes.put(CrashReporterAttributes.SPLUNK_OPERATION_KEY, properties.name)
    attributes.put(SemanticAttributes.THREAD_ID, thread.id)
    attributes.put(SemanticAttributes.THREAD_NAME, thread.name)
    attributes.put(SemanticAttributes.EXCEPTION_ESCAPED, true)
    attributes.put(
      CrashReporterAttributes.STORAGE_SPACE_FREE_KEY,
      runtimeDetailsExtractor.getCurrentStorageFreeSpaceInBytes()
    )
    attributes.put(
      CrashReporterAttributes.HEAP_FREE_KEY,
      runtimeDetailsExtractor.getCurrentFreeHeapInBytes()
    )

    sessionId?.let {
      attributes.put("splunk.rumSessionId", it)
    }

    runtimeDetailsExtractor.getCurrentBatteryPercent()?.let {
      attributes.put(CrashReporterAttributes.BATTERY_PERCENT_KEY, it)
    }

    val component = if (crashHappened.compareAndSet(false, true)) {
      CrashReporterAttributes.COMPONENT_CRASH
    } else {
      CrashReporterAttributes.COMPONENT_ERROR
    }
    attributes.put(CrashReporterAttributes.COMPONENT_KEY, component)

    val network = currentNetworkProvider.refreshNetworkStatus()
    val networkAttributesExtractor = CurrentNetworkAttributesExtractor()
    val networkAttributes = networkAttributesExtractor.extract(network)
    attributes.putAll(networkAttributes)

    return attributes.build()
  }

  private fun buildEvents(exception: Throwable, epochNanos: Long, attributes: Attributes): List<EventData> {
    val event = ExceptionEventData
      .create(epochNanos, exception, attributes, attributes.size())
    return listOf(event)
  }

  private fun buildProperties(exception: Throwable, epochNanos: Long): ReactSpanProperties {
    val name = exception.javaClass.simpleName
    return ReactSpanProperties(
      name = name,
      kind = SpanKind.INTERNAL,
      status = StatusData.error(),
      startTimeNanos = epochNanos,
      endTimeNanos = epochNanos
    )
  }
}

