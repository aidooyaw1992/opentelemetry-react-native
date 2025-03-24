package com.opentelemetryreactnative.crash

import io.opentelemetry.sdk.common.Clock

// copied from otel-java
internal class AnchoredClock private constructor(
  private val clock: Clock,
  private val epochNanos: Long,
  private val nanoTime: Long
) {
  fun now(): Long {
    val deltaNanos = this.clock.nanoTime() - this.nanoTime
    return this.epochNanos + deltaNanos
  }

  companion object {
    fun create(clock: Clock): AnchoredClock {
      return AnchoredClock(clock, clock.now(), clock.nanoTime())
    }
  }
}
