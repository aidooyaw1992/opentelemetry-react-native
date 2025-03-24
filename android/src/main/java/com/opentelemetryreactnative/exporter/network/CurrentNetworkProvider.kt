package com.opentelemetryreactnative.exporter.network

import android.Manifest
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.opentelemetryreactnative.LogConstants
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Supplier

class CurrentNetworkProvider private constructor(
  private val networkDetector: NetworkDetector
) {

  @Volatile
  private var currentNetwork: CurrentNetwork = UNKNOWN_NETWORK

  private val listeners = CopyOnWriteArrayList<NetworkChangeListener>()

  // visible for tests
  @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
  fun startMonitoring(
    createNetworkMonitoringRequest: Supplier<NetworkRequest>,
    connectivityManager: ConnectivityManager
  ) {
    refreshNetworkStatus()
    try {
      registerNetworkCallbacks(createNetworkMonitoringRequest, connectivityManager)
    } catch (e: Exception) {
      // if this fails, we'll go without network change events.
      Log.w(
        LogConstants.LOG_TAG,
        "Failed to register network callbacks. Automatic network monitoring is disabled.",
        e
      )
    }
  }

  @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
  private fun registerNetworkCallbacks(
    createNetworkMonitoringRequest: Supplier<NetworkRequest>,
    connectivityManager: ConnectivityManager
  ) {
    connectivityManager.registerDefaultNetworkCallback(ConnectionMonitor())
  }

  /** Returns up-to-date [CurrentNetwork] current network information. */
  fun refreshNetworkStatus(): CurrentNetwork {
    try {
      currentNetwork = networkDetector.detectCurrentNetwork()
    } catch (e: Exception) {
      // guard against security issues/bugs when accessing the Android connectivityManager.
      // see: https://issuetracker.google.com/issues/175055271
      currentNetwork = UNKNOWN_NETWORK
    }
    return currentNetwork
  }

  fun getCurrentNetwork(): CurrentNetwork {
    return currentNetwork
  }

  fun addNetworkChangeListener(listener: NetworkChangeListener) {
    listeners.add(listener)
  }

  private fun notifyListeners(activeNetwork: CurrentNetwork) {
    for (listener in listeners) {
      listener.onNetworkChange(activeNetwork)
    }
  }

  private inner class ConnectionMonitor : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
      val activeNetwork = refreshNetworkStatus()
      Log.d(LogConstants.LOG_TAG, "  onAvailable: currentNetwork=$activeNetwork")

      notifyListeners(activeNetwork)
    }

    override fun onLost(network: Network) {
      // it seems that the "currentNetwork" is still the one that is being lost, so for
      // this method, we'll force it to be NO_NETWORK, rather than relying on the
      // ConnectivityManager to have the right
      // state at the right time during this event.
      val currentNetwork = NO_NETWORK
      this@CurrentNetworkProvider.currentNetwork = currentNetwork
      Log.d(LogConstants.LOG_TAG, "  onLost: currentNetwork=$currentNetwork")

      notifyListeners(currentNetwork)
    }
  }

  companion object {
    val NO_NETWORK = CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build()
    val UNKNOWN_NETWORK = CurrentNetwork.builder(NetworkState.TRANSPORT_UNKNOWN).build()

    /**
     * Creates a new [CurrentNetworkProvider] instance and registers network callbacks in the
     * Android [ConnectivityManager].
     */

    fun createAndStart(application: Application): CurrentNetworkProvider {
      val context = application.applicationContext
      val currentNetworkProvider =
        CurrentNetworkProvider(NetworkDetector.create(context))
      currentNetworkProvider.startMonitoring(
        { createNetworkMonitoringRequest() },
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      )
      return currentNetworkProvider
    }

    private fun createNetworkMonitoringRequest(): NetworkRequest {
      // note: this throws an NPE when running in junit without robolectric, due to Android
      return NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
        .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
        .build()
    }
  }
}
