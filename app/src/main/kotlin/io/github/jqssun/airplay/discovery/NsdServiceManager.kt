package io.github.jqssun.airplay.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log

data class MdnsService(val name: String, val type: String, val port: Int, val txt: Map<String, String>)

class NsdServiceManager(private val ctx: Context) {

    private val nsdManager = ctx.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var multicastLock: WifiManager.MulticastLock? = null
    private val registrations = mutableListOf<NsdManager.RegistrationListener>()
    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    fun acquireMulticastLock() {
        val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("airplay_mdns").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    /** Called (main thread) when a service had to take a new name because another device or app on this TV holds it. */
    var onRenamed: ((type: String, newName: String) -> Unit)? = null

    fun register(services: List<MdnsService>) = services.forEach { _register(it, 1) }

    // Some TVs (e.g. MediaTek with built-in AirPlay, or with AirScreen installed) already advertise the TV's name.
    // Android then silently drops our registration (NameConflictException, often no failure callback),
    // so retry under "Name (2)", "Name (3)"... when it isn't confirmed in time.
    private fun _register(s: MdnsService, attempt: Int) {
        val name = nameForAttempt(s.name, attempt)
        val info = NsdServiceInfo().apply {
            serviceName = name
            serviceType = s.type
            port = s.port
            s.txt.forEach { (k, v) -> setAttribute(k, v) }
        }
        var settled = false
        lateinit var listener: NsdManager.RegistrationListener
        fun retry(reason: String) {
            if (settled) return
            settled = true
            Log.w(TAG, "${s.type} '$name' not registered ($reason)")
            try { nsdManager.unregisterService(listener) } catch (_: Exception) {}
            registrations -= listener
            if (attempt < MAX_ATTEMPTS) _register(s, attempt + 1)
        }
        val timeout = Runnable { retry("timeout") }
        listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                main.post {
                    if (settled) return@post
                    settled = true
                    main.removeCallbacks(timeout)
                    Log.i(TAG, "${s.type} registered: ${info.serviceName}")
                    if (attempt > 1 || info.serviceName != s.name) onRenamed?.invoke(s.type, info.serviceName)
                }
            }
            override fun onRegistrationFailed(info: NsdServiceInfo, code: Int) {
                main.post { main.removeCallbacks(timeout); retry("failed $code") }
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.i(TAG, "${s.type} unregistered")
            }
            override fun onUnregistrationFailed(info: NsdServiceInfo, code: Int) {
                Log.e(TAG, "${s.type} unregister failed: $code")
            }
        }
        nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        registrations += listener
        main.postDelayed(timeout, REGISTER_TIMEOUT_MS)
    }

    fun release() {
        registrations.forEach {
            try { nsdManager.unregisterService(it) } catch (_: Exception) {}
        }
        registrations.clear()
        main.removeCallbacksAndMessages(null)
        multicastLock?.release()
        multicastLock = null
    }

    companion object {
        private const val TAG = "NsdServiceManager"
        private const val MAX_ATTEMPTS = 5
        private const val REGISTER_TIMEOUT_MS = 6_000L

        /** "Living Room TV" -> "Living Room TV (2)" on the 2nd attempt; keeps a raop "HW@" prefix intact. */
        fun nameForAttempt(base: String, attempt: Int): String = if (attempt <= 1) base else "$base ($attempt)"
    }
}
