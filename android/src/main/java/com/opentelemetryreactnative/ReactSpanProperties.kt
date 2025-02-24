package com.opentelemetryreactnative

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.sdk.trace.data.StatusData

data class ReactSpanProperties(
  val name: String,
  val tracerName: String,
  val kind: SpanKind,
  val status: StatusData,
  val startTimeNanos: Long,
  val endTimeNanos: Long
)
