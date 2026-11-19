package com.focusguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
        }
    }

    companion object {
        private const val TAG = "FocusGuard_PkgReceiver"
    }
}
