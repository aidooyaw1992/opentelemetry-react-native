package com.opentelemetryreactnative

import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType

abstract class MapReader {
  protected class StringKey(private val key: String) {
    fun get(map: ReadableMap): String? {
      return if (map.hasKey(key) && map.getType(key) == ReadableType.String) {
        map.getString(key)
      } else {
        null
      }
    }
  }

  protected class BooleanKey(private val key: String) {
    fun get(map: ReadableMap): Boolean? {
      return if (map.hasKey(key) && map.getType(key) == ReadableType.Boolean) {
        map.getBoolean(key)
      } else {
        null
      }
    }
  }

  protected class NumberKey(private val key: String) {
    fun getLong(map: ReadableMap): Long? {
      return if (map.hasKey(key) && map.getType(key) == ReadableType.Number) {
        map.getDouble(key).toLong()
      } else {
        null
      }
    }
  }

  protected class MapKey(private val key: String) {
    fun getMap(map: ReadableMap): ReadableMap? {
      return if (map.hasKey(key) && map.getType(key) == ReadableType.Map) {
        map.getMap(key)
      } else {
        null
      }
    }
  }
}
