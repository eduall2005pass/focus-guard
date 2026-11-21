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

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "com.focusguard.action.CLEAR_INSTALL_RESTRICTION") {
            Log.i(TAG, "Received CLEAR_INSTALL_RESTRICTION maintenance broadcast. Temporarily clearing install restrictions.")
            val controller = DevicePolicyController(context)
            if (controller.isDeviceOwner()) {
                controller.clearInstallRestrictionForUpdate()
            }
            return
        }

        if (action == "com.focusguard.action.ENFORCE_POLICY") {
            Log.i(TAG, "Received ENFORCE_POLICY maintenance broadcast. Enforcing all policies.")
            val controller = DevicePolicyController(context)
            if (controller.isDeviceOwner()) {
                val repository = AllowlistRepository(context)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        val customAllowed = repository.customAllowedPackagesFlow.first()
                        controller.enforceAllPolicies(customAllowed)
                        Log.i(TAG, "Maintenance policy enforcement completed successfully.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error enforcing policies on maintenance broadcast", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            return
        }

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "Received boot/replacement broadcast: $action. Re-asserting policies.")

            val controller = DevicePolicyController(context)
            if (!controller.isDeviceOwner()) {
                Log.d(TAG, "Not device owner. Skipping boot enforcement.")
                return
            }

            // Anti-Safe-Mode Trap: If device ever enters Safe Mode, reboot immediately
            if (controller.checkAndRemediateSafeMode()) {
                Log.w(TAG, "Safe Mode intercepted! Device reboot triggered back to normal mode.")
                return
            }

            val repository = AllowlistRepository(context)
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    kotlinx.coroutines.withTimeoutOrNull(6000L) {
                        val customAllowed = repository.customAllowedPackagesFlow.first()
                        controller.enforceAllPolicies(customAllowed)
                        Log.i(TAG, "Boot policy enforcement completed successfully.")
                    } ?: Log.w(TAG, "Boot policy enforcement exceeded time limit; released receiver.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error enforcing policies on boot", e)
                } finally {
                    try {
                        pendingResult.finish()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    companion object {
        private const val TAG = "FocusGuard_BootReceiver"
    }
}
