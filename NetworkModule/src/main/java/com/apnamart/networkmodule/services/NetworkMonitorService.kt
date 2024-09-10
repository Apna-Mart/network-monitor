package com.apnamart.networkmodule.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.MutableLiveData
import com.apnamart.networkmodule.network_monitor.InternetAvailableCallback
import com.apnamart.networkmodule.network_monitor.NetworkMonitor

abstract class NetworkMonitorService : Service() {
    val lastNetworkState = MutableLiveData<Boolean>()
    private var networkMonitor: NetworkMonitor? = null

    abstract fun onNetworkAvailable()

    abstract fun onNetworkLost(
        isAirplaneModeOn: Boolean,
        isNetworkAvailable: Boolean,
        isWifiEnabled: Boolean,
        isMobileDataEnabled: Boolean
    )

    override fun onCreate() {
        super.onCreate()
        networkMonitor = NetworkMonitor(this)
        registerNetworkCallback()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor?.unregisterCallback()
        networkMonitor = null
    }

    private fun registerNetworkCallback() {
        val context = this@NetworkMonitorService
        networkMonitor?.registerNetworkCallback(
            object : InternetAvailableCallback {
                override fun onNetworkAvailable() {
                    context.onNetworkAvailable()
                    lastNetworkState.postValue(true)
                }

                override fun onNetworkLost(
                    isAirplaneModeOn: Boolean,
                    isNetworkAvailable: Boolean,
                    isWifiEnabled: Boolean,
                    isMobileDataEnabled: Boolean
                ) {
                    context.onNetworkLost(
                        isAirplaneModeOn, isNetworkAvailable, isWifiEnabled, isMobileDataEnabled
                    )
                    lastNetworkState.postValue(false)
                }
            }
        )
    }

}