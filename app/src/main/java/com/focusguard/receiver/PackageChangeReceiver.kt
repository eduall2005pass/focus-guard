package com.focusguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.focusguard.data.repository.AllowlistRepository
import com.focusguard.dpc.DevicePolicyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val packageName = intent.data?.schemeSpecificPart ?: return

        if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
            Log.i(TAG, "Package change detected: $action for $packageName")

            val controller = DevicePolicyController(context)
            if (!controller.isDeviceOwner()) return

            // 1. FAST SYNCHRONOUS SUSPENSION (< 2ms)
            // Immediately suspend unauthorized packages before any background coroutine or DataStore read
            val suspended = controller.suspendImmediatelyIfUnauthorized(packageName)
            if (suspended) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        val appLabel = try {
                            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                            context.packageManager.getApplicationLabel(appInfo).toString()
                        } catch (_: Exception) { packageName }

                        Toast.makeText(
                            context,
                            "⚠️ Focus Guard: '$appLabel' অনুমোদিত নয় এবং লক করা হয়েছে। আপনি চাইলে এটি আনইনস্টল করে দিতে পারেন।",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (_: Exception) {}
                }
            }

            val repository = AllowlistRepository(context)
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val customAllowed = repository.customAllowedPackagesFlow.first()
                    controller.evaluatePackage(packageName, customAllowed)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to evaluate package $packageName", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == Intent.ACTION_PACKAGE_REMOVED || action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
            val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            if (!isReplacing) {
                Log.i(TAG, "Package uninstall detected: $packageName. Preserving record in Approved Registry for future reinstall.")
                val controller = DevicePolicyController(context)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        controller.onPackageUninstalled(packageName)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to handle uninstall event for $packageName", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "FocusGuard_PkgReceiver"
    }
}
