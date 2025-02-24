package com.opentelemetryreactnative

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log

private const val TAG = "PerfProvider"
class PerfProvider: ContentProvider() {

  override fun onCreate(): Boolean {
    Log.d(TAG, "onCreate: perfProvider onCreate: $appStartTime")
    val activityCallbacks = ActivityCallbacks()
    activityCallbacks.registerActivityLifecycleCallbacks(context!!)
    //TODO deregister callbacks?
    return false
  }

  override fun query(
    p0: Uri,
    p1: Array<out String>?,
    p2: String?,
    p3: Array<out String>?,
    p4: String?
  ): Cursor? {
    TODO("Not yet implemented")
  }

  override fun getType(p0: Uri): String? = null

  override fun insert(p0: Uri, p1: ContentValues?): Uri?  = null

  override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int = 0

  override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int = 0


  companion object {
    private val appStartTime: Long = System.currentTimeMillis()
    fun getAppStartTime(): Long = appStartTime
  }
}
