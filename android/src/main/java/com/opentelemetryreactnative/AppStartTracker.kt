package com.opentelemetryreactnative

class AppStartTracker private constructor() {
  var isColdStart: Boolean = false

  companion object {
    @JvmStatic
    val instance: AppStartTracker = AppStartTracker()
  }
}
