package com.opentelemetryreactnative

import com.facebook.react.bridge.ReadableMap

class SpanMapReader(val map: ReadableMap) : MapReader() {
  val name: String?
    get() = Keys.NAME.get(map)

  val traceId: String?
    get() = Keys.TRACE_ID.get(map)

  val tracerName: String?
    get() = Keys.TRACER_NAME.get(map)

  val traceFlags: Long?
    get() = Keys.TRACE_FLAGS.getLong(map)

  val spanId: String?
    get() = Keys.SPAN_ID.get(map)

  val parentSpanId: String?
    get() = Keys.PARENT_SPAN_ID.get(map)

  val startEpochMillis: Long?
    get() = Keys.START_TIME.getLong(map)

  val endEpochMillis: Long?
    get() = Keys.END_TIME.getLong(map)

  val attributes: ReadableMap?
    get() = Keys.ATTRIBUTES.getMap(map)

  private interface Keys {
    companion object {
      val NAME = StringKey("name")
      val TRACE_ID = StringKey("traceId")
      val TRACER_NAME = StringKey("tracerName")
      val TRACE_FLAGS = NumberKey("traceFlags")
      val SPAN_ID = StringKey("spanId")
      val PARENT_SPAN_ID = StringKey("parentSpanId")
      val START_TIME = NumberKey("startTime")
      val END_TIME = NumberKey("endTime")
      val ATTRIBUTES = MapKey("attributes")
    }
  }
}
