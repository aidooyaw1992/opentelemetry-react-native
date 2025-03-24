package com.opentelemetryreactnative.crash

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.DelegatingSpanData
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter

import io.opentelemetry.semconv.trace.attributes.SemanticAttributes

class CrashEventAttributeExtractor(private val delegate: SpanExporter): SpanExporter {
  companion object {
    val ERROR_TYPE_KEY: AttributeKey<String> = stringKey("error.type")
    val ERROR_MESSAGE_KEY: AttributeKey<String> = stringKey("error.message")
  }
  override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
    val modifiedSpans = spans.map { span -> modify(span) }
    return delegate.export(modifiedSpans)
  }

  private fun modify(original: SpanData): SpanData {
    if (original.events.isEmpty()) {
      return original
    }

    val modifiedEvents = mutableListOf<EventData>()
    val modifiedAttributes = original.attributes.toBuilder()

    // zipkin eats the event attributes that are recorded by default, so we need to convert
    // the exception event to span attributes
    for (event in original.events) {
      if (event.name == SemanticAttributes.EXCEPTION_EVENT_NAME) {
        modifiedAttributes.putAll(extractExceptionAttributes(event))
      } else {
        // if it's not an exception, leave the event as it is
        modifiedEvents.add(event)
      }
    }

    return SplunkSpan(original, modifiedEvents, modifiedAttributes.build())
  }

  private fun extractExceptionAttributes(event: EventData): Attributes {
    val type = event.attributes.get(SemanticAttributes.EXCEPTION_TYPE)
    val message = event.attributes.get(SemanticAttributes.EXCEPTION_MESSAGE)
    val stacktrace = event.attributes.get(SemanticAttributes.EXCEPTION_STACKTRACE)
    val builder = Attributes.builder()

    type?.let {
      val dot = it.lastIndexOf('.')
      val simpleType = if (dot == -1) it else it.substring(dot + 1)
      builder.put(SemanticAttributes.EXCEPTION_TYPE, simpleType)
      // this attribute's here to support the RUM UI/backend until it can be updated to use
      // otel conventions.
      builder.put(ERROR_TYPE_KEY, simpleType)
    }

    message?.let {
      builder.put(SemanticAttributes.EXCEPTION_MESSAGE, it)
      // this attribute's here to support the RUM UI/backend until it can be updated to use
      // otel conventions.
      builder.put(ERROR_MESSAGE_KEY, it)
    }

    stacktrace?.let {
      builder.put(SemanticAttributes.EXCEPTION_STACKTRACE, it)
    }

    return builder.build()
  }
  override fun flush(): CompletableResultCode {
    return delegate.flush()
  }

  override fun shutdown(): CompletableResultCode {
    return delegate.shutdown()
  }
}


private class SplunkSpan(
  delegate: SpanData,
  private val modifiedEvents: List<EventData>,
  private val modifiedAttributes: Attributes
) : DelegatingSpanData(delegate) {

  override fun getEvents(): List<EventData> {
    return modifiedEvents
  }

  override fun getTotalRecordedEvents(): Int {
    return modifiedEvents.size
  }

  override fun getAttributes(): Attributes {
    return modifiedAttributes
  }

  override fun getTotalAttributeCount(): Int {
    return modifiedAttributes.size()
  }
}
