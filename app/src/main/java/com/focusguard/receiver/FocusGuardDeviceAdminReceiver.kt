package com.focusguard.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.focusguard.data.repository.AllowlistRepository
import com.focusguard.dpc.DevicePolicyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusGuardDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.i(TAG, "Device Owner provisioning complete. Triggering initial policy enforcement.")
        Toast.makeText(context, "Focus Guard: Device Owner Provisioned", Toast.LENGTH_LONG).show()

        val controller = DevicePolicyController(context)
        val repository = AllowlistRepository(context)

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val customAllowed = repository.customAllowedPackagesFlow.first()
                controller.enforceAllPolicies(customAllowed)
            } catch (e: Exception) {
                Log.e(TAG, "Failed initial enforcement after provisioning", e)
            }
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Focus Guard Device Admin enabled.")
        val controller = DevicePolicyController(context)
        controller.checkAndRemediateSafeMode()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "Focus Guard Device Admin disabled.")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling Focus Guard will compromise device distraction protections."
    }

    companion object {
        private const val TAG = "FocusGuard_DAReceiver"
    }
}
