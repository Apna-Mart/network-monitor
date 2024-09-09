package com.apnamart.networkmodule.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.lifecycle.MutableLiveData

abstract class NetworkMonitorService : Service() {
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    val lastNetworkState = MutableLiveData<Boolean>()

    abstract fun onNetworkAvailable()

    abstract fun onNetworkLost(
        isAirplaneModeOn: Boolean,
        isNetworkAvailable: Boolean,
        isWifiEnabled: Boolean,
        isMobileDataEnabled: Boolean
    )

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        registerNetworkCallback()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun registerNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onNetworkAvailable()
                lastNetworkState.postValue(true)
            }

            override fun onLost(network: Network) {
                lastNetworkState.postValue(false)
                val isNetworkConnected = checkNetworkAvailability(this@NetworkMonitorService)
                if ((isNetworkConnected)) {
                    return
                }

                onNetworkLost(
                    isAirplaneModeOn(this@NetworkMonitorService),
                    false,
                    isWifiEnabled(this@NetworkMonitorService),
                    isMobileDataEnabled(this@NetworkMonitorService)
                )
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun checkNetworkAvailability(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun isAirplaneModeOn(context: Context): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0
    }

    fun isWifiEnabled(context: Context): Boolean {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    fun isMobileDataEnabled(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use ConnectivityManager for API 29+ to get network capabilities
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } else {
            // Older API versions
            try {
                val clazz = Class.forName(connectivityManager.javaClass.name)
                val method = clazz.getDeclaredMethod("getMobileDataEnabled")
                method.isAccessible = true
                return method.invoke(connectivityManager) as Boolean
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
    }
}