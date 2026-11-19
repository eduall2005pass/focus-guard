package com.focusguard.data.model

data class PolicyEnforcementResult(
    val timestamp: Long = System.currentTimeMillis(),
    val totalPackagesScanned: Int = 0,
    val packagesHidden: Int = 0,
    val packagesSuspended: Int = 0,
    val packagesProtected: Int = 0,
    val restrictionsApplied: List<String> = emptyList(),
    val privateDnsStatus: String = "Not Configured",
    val errors: List<String> = emptyList()
)

data class BaselineAppRecord(
    val packageName: String,
    val initialVersionCode: Long,
    val initialVersionName: String,
    val isSystemApp: Boolean,
    val installingPackageName: String?,
    val signingCertificateSha256: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DeviceOwnerStatus(
    val isDeviceOwner: Boolean = false,
    val isDeviceAdmin: Boolean = false,
    val adminComponent: String = "",
    val privateDnsMode: Int = 0,
    val privateDnsHost: String? = null,
    val isPrivateDnsLocked: Boolean = false,
    val isVpnRestricted: Boolean = false,
    val isUninstallBlocked: Boolean = false,
    val isSafeBootDisallowed: Boolean = false,
    val isUnknownSourcesBlocked: Boolean = false,
    val isAdbDebuggingBlocked: Boolean = false,
    val isPolicyPermanentlyLocked: Boolean = false,
    val baselinePackageCount: Int = 0,
    val activeRestrictions: List<String> = emptyList()
)
