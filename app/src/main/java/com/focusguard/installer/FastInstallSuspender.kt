package com.focusguard.installer

import android.content.Context
import android.content.pm.PackageInstaller
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.focusguard.dpc.DevicePolicyController
import java.util.concurrent.ConcurrentHashMap

/**
 * FastInstallSuspender listens directly to Android's PackageInstaller framework callbacks.
 * The instant an APK installation session commits and finishes in the OS, this callback fires,
 * suspending unauthorized applications in 0ms before Play Store or external launchers
 * can present an active "Open" button to the user.
 */
class FastInstallSuspender private constructor(private val context: Context) {

    private val packageInstaller: PackageInstaller = context.packageManager.packageInstaller
    private val controller = DevicePolicyController(context)
    private val sessionPackageMap = ConcurrentHashMap<Int, String>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isRegistered = false

    private val sessionCallback = object : PackageInstaller.SessionCallback() {
        override fun onCreated(sessionId: Int) {
            recordSession(sessionId)
        }

        override fun onBadgingChanged(sessionId: Int) {}

        override fun onActiveChanged(sessionId: Int, active: Boolean) {
            if (active) recordSession(sessionId)
        }

        override fun onProgressChanged(sessionId: Int, progress: Float) {
            recordSession(sessionId)
        }

        override fun onFinished(sessionId: Int, success: Boolean) {
            val packageName = sessionPackageMap.remove(sessionId)
                ?: try {
                    packageInstaller.getSessionInfo(sessionId)?.appPackageName
                } catch (_: Exception) { null }

            Log.i(TAG, "PackageInstaller session finished: sessionId=$sessionId, pkg=$packageName, success=$success")
            if (success && !packageName.isNullOrBlank()) {
                mainHandler.post {
                    val suspended = controller.suspendImmediatelyIfUnauthorized(packageName)
                    if (suspended) {
                        try {
                            val appLabel = try {
                                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                                context.packageManager.getApplicationLabel(appInfo).toString()
                            } catch (_: Exception) { packageName }
                            Toast.makeText(
                                context,
                                "⚠️ Focus Guard: '$appLabel' অনুমোদিত নয় এবং সাথে সাথে লক করা হয়েছে।",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        private fun recordSession(sessionId: Int) {
            if (sessionPackageMap.containsKey(sessionId)) return
            try {
                val info = packageInstaller.getSessionInfo(sessionId)
                val pkg = info?.appPackageName
                if (!pkg.isNullOrBlank()) {
                    sessionPackageMap[sessionId] = pkg
                    Log.d(TAG, "Tracked install session: $sessionId -> $pkg")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error inspecting session $sessionId: ${e.message}")
            }
        }
    }

    @Synchronized
    fun startWatching() {
        if (isRegistered) return
        try {
            // Track existing sessions currently in progress
            try {
                for (info in packageInstaller.allSessions) {
                    val pkg = info.appPackageName
                    if (!pkg.isNullOrBlank()) {
                        sessionPackageMap[info.sessionId] = pkg
                    }
                }
            } catch (_: Exception) {}

            packageInstaller.registerSessionCallback(sessionCallback, mainHandler)
            isRegistered = true
            Log.i(TAG, "FastInstallSuspender registered successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register PackageInstaller session callback", e)
        }
    }

    companion object {
        private const val TAG = "FocusGuard_FastInstall"

        @Volatile
        private var instance: FastInstallSuspender? = null

        fun getInstance(context: Context): FastInstallSuspender {
            return instance ?: synchronized(this) {
                instance ?: FastInstallSuspender(context.applicationContext).also { instance = it }
            }
        }
    }
}
