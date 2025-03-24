package com.opentelemetryreactnative

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes

class ReactSpanData(
  private val properties: ReactSpanProperties,
  private val attributes: Attributes,
  private val context: SpanContext,
  private val parentContext: SpanContext,
  private val events: List<EventData>
) : SpanData {

    override fun getName(): String {
        return properties.name
    }

    override fun getKind(): SpanKind {
        return properties.kind
    }

    override fun getSpanContext(): SpanContext {
        return context
    }

    override fun getParentSpanContext(): SpanContext {
        return parentContext
    }

    override fun getStatus(): StatusData {
        return properties.status
    }

    override fun getStartEpochNanos(): Long {
        return properties.startTimeNanos
    }

    override fun getAttributes(): Attributes {
        return attributes
    }

    override fun getEvents(): List<EventData> {
        return events
    }

    override fun getLinks(): List<LinkData> {
        return emptyList()
    }

    override fun getEndEpochNanos(): Long {
        return properties.endTimeNanos
    }

    override fun hasEnded(): Boolean {
        return true
    }

    override fun getTotalRecordedEvents(): Int {
        return 0
    }

    override fun getTotalRecordedLinks(): Int {
        return 0
    }

    override fun getTotalAttributeCount(): Int {
        return attributes.size()
    }

    @Deprecated("Deprecated in Java")
    override fun getInstrumentationLibraryInfo(): InstrumentationLibraryInfo {
        return InstrumentationLibraryInfo.empty()
    }

    override fun getResource(): Resource {
        return Resource.getDefault().merge(
            Resource.create(
                Attributes.of(
                    ResourceAttributes.SERVICE_NAME, "mymtn-app"
                )
            )
        )
    }
}
