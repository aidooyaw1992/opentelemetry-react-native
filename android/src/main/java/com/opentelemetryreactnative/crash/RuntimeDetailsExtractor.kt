package com.opentelemetryreactnative.crash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.File

// partial copy of splunk-otel-android
internal class RuntimeDetailsExtractor private constructor(
  private val filesDir: File
) : BroadcastReceiver() {
  @Volatile
  private var batteryPercent: Double? = null

  override fun onReceive(context: Context, intent: Intent) {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    batteryPercent = level * 100.0 / scale.toFloat()
  }

  fun getCurrentStorageFreeSpaceInBytes(): Long {
    return filesDir.freeSpace
  }

  fun getCurrentFreeHeapInBytes(): Long {
    val runtime = Runtime.getRuntime()
    return runtime.freeMemory()
  }

  fun getCurrentBatteryPercent(): Double? {
    return batteryPercent
  }

  companion object {
    fun create(context: Context): RuntimeDetailsExtractor {
      val batteryChangedFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
      val filesDir = context.filesDir
      val runtimeDetails = RuntimeDetailsExtractor(filesDir)
      context.registerReceiver(runtimeDetails, batteryChangedFilter)
      return runtimeDetails
    }
  }
}
