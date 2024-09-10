package com.apnamart.networkmodule.network_monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

class NetworkMonitor(val context: Context) {

    var connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    lateinit var networkCallback: ConnectivityManager.NetworkCallback

    fun registerNetworkCallback(callback: InternetAvailableCallback) {
        networkCallback = object : ConnectivityManager.NetworkCallback(){
            override fun onAvailable(network: Network) {
                callback.onNetworkAvailable()
            }

            override fun onLost(network: Network) {
                val isNetworkConnected = checkNetworkAvailability(context)
                if (isNetworkConnected) {
                    return
                }

                callback.onNetworkLost(
                    isAirplaneModeOn(context),
                    false,
                    isWifiEnabled(context),
                    isMobileDataEnabled(context)
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

interface InternetAvailableCallback {

    fun onNetworkAvailable()
    fun onNetworkLost(
        isAirplaneModeOn: Boolean,
        isNetworkAvailable: Boolean,
        isWifiEnabled: Boolean,
        isMobileDataEnabled: Boolean
    )
}