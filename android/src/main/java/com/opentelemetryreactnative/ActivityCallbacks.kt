package com.opentelemetryreactnative


import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log


class ActivityCallbacks: Application.ActivityLifecycleCallbacks {

  private var isRegisteredForLifecycleCallbacks = false

  fun registerActivityLifecycleCallbacks(context: Context) {
    // Make sure the callback is registered only once.
    if (isRegisteredForLifecycleCallbacks) {
      return
    }

    context.applicationContext.let { appContext ->
      (appContext as? Application)?.also { app ->
        app.registerActivityLifecycleCallbacks(this)
        isRegisteredForLifecycleCallbacks = true
      }
    }
  }
  override fun onActivityCreated(p0: Activity, savedInstanceState: Bundle?) {
    val isColdStart = savedInstanceState == null
    Log.d("SplunkRNRum", "onActivityCreated: $isColdStart")
    AppStartTracker.instance.isColdStart = isColdStart
  }

  override fun onActivityStarted(p0: Activity) {
    TODO("Not yet implemented")
  }

  override fun onActivityResumed(activity: Activity) {
    val simpleName = activity::class.java.simpleName
    Log.d("OTEL", "onActivityResumed: $simpleName")
  }

  override fun onActivityPaused(p0: Activity) {
    TODO("Not yet implemented")
  }

  override fun onActivityStopped(p0: Activity) {
    TODO("Not yet implemented")
  }

  override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
    TODO("Not yet implemented")
  }

  override fun onActivityDestroyed(p0: Activity) {
    TODO("Not yet implemented")
  }
}
