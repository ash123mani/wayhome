package com.wayhome.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

data class ConnectivityState(
    val internetAvailable: Boolean,
    val nearbyActive: Boolean
) {
    fun banner(): String = when {
        nearbyActive && internetAvailable -> "🟢 Nearby mode active"
        nearbyActive -> "🔴 Internet unavailable · 🟢 Offline nearby mode active"
        else -> "⚪ Nearby off — tap “Find people going my way”"
    }
}

@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun observe(nearbyActiveFlow: Flow<Boolean>): Flow<ConnectivityState> = flow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        while (true) {
            val net = cm.activeNetwork
            val caps = net?.let { cm.getNetworkCapabilities(it) }
            val internet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            // nearbyActiveFlow is hot; sample latest via variable in ViewModel instead.
            // Here emit internet part; ViewModel combines. Emit placeholder combined below.
            emit(ConnectivityState(internet, false))
            delay(4000)
        }
    }

    fun hasInternetNow(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) } ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
