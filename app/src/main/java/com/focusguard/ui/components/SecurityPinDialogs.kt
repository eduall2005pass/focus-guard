package com.focusguard.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.data.security.SecurityPinManager
import com.focusguard.dpc.DevicePolicyController
import com.focusguard.ui.theme.*

@Composable
fun SetSecurityPinDialog(
    pinManager: SecurityPinManager = SecurityPinManager(LocalContext.current),
    allowDismiss: Boolean = false,
    onPinSetSuccess: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (allowDismiss) onDismiss()
        },
        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GuardGreenText) },
        title = {
            Text(
                text = "সিকিউরিটি পিন সেট করুন",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Focus Guard প্যারেন্টাল কন্ট্রোল এবং সিকিউরিটি সেটিংস অ্যাক্সেস সুরক্ষিত রাখতে একটি ৪-৮ ডিজিটের সিকিউরিটি পিন তৈরি করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= SecurityPinManager.MAX_PIN_LENGTH && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("নতুন পিন (৪-৮ ডিজিট)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= SecurityPinManager.MAX_PIN_LENGTH && it.all { char -> char.isDigit() }) {
                            confirmPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("পিন পুনরায় লিখুন (Confirm PIN)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length < SecurityPinManager.MIN_PIN_LENGTH) {
                        errorMessage = "পিন কমপক্ষে ৪ ডিজিটের হতে হবে"
                        return@Button
                    }
                    if (pin != confirmPin) {
                        errorMessage = "দুটো পিন মিলছে না!"
                        return@Button
                    }
                    if (pinManager.setPin(pin)) {
                        Toast.makeText(context, "সিকিউরিটি পিন সফলভাবে সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                        onPinSetSuccess()
                    } else {
                        errorMessage = "পিন সেভ করা সম্ভব হয়নি"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText)
            ) {
                Text("পিন সেভ করুন", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = if (allowDismiss) {
            {
                TextButton(onClick = onDismiss) {
                    Text("বাতিল", color = TextMuted)
                }
            }
        } else null,
        containerColor = DarkSurface,
        textContentColor = TextPrimary
    )
}

@Composable
fun EnterSecurityPinDialog(
    pinManager: SecurityPinManager = SecurityPinManager(LocalContext.current),
    onPinVerified: () -> Unit,
    onCancel: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Default.Password, contentDescription = null, tint = GuardGreenText) },
        title = {
            Text(
                text = "সিকিউরিটি পিন দিন",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "প্যারেন্টাল কন্ট্রোল সেটিংস অ্যাক্সেস করতে আপনার সিকিউরিটি পিন প্রদান করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= SecurityPinManager.MAX_PIN_LENGTH && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("সিকিউরিটি পিন") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinManager.verifyPin(pin)) {
                        onPinVerified()
                    } else {
                        pin = ""
                        errorMessage = "ভুল পিন! সঠিক সিকিউরিটি পিন দিন।"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText)
            ) {
                Text("আনলক করুন", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("বাতিল", color = TextMuted)
            }
        },
        containerColor = DarkSurface,
        textContentColor = TextPrimary
    )
}

@Composable
fun ChangeSecurityPinDialog(
    pinManager: SecurityPinManager = SecurityPinManager(LocalContext.current),
    allowDismiss: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Password, contentDescription = null, tint = GuardGreenText) },
        title = {
            Text(
                text = "সিকিউরিটি পিন পরিবর্তন",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = {
                        if (it.length <= SecurityPinManager.MAX_PIN_LENGTH && it.all { char -> char.isDigit() }) {
                            oldPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("বর্তমান পিন (Old PIN)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= SecurityPinManager.MAX_PIN_LENGTH && it.all { char -> char.isDigit() }) {
                            newPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("নতুন পিন (New PIN)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = {
                        if (it.length <= SecurityPinManager.MAX_PIN_LENGTH && it.all { char -> char.isDigit() }) {
                            confirmNewPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("নতুন পিন পুনরায় লিখুন (Confirm)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!pinManager.verifyPin(oldPin)) {
                        errorMessage = "বর্তমান পিনটি সঠিক নয়"
                        return@Button
                    }
                    if (newPin.length < SecurityPinManager.MIN_PIN_LENGTH) {
                        errorMessage = "নতুন পিন কমপক্ষে ৪ ডিজিট হতে হবে"
                        return@Button
                    }
                    if (newPin != confirmNewPin) {
                        errorMessage = "নতুন পিন দুটো মিলছে না"
                        return@Button
                    }
                    if (pinManager.changePin(oldPin, newPin)) {
                        Toast.makeText(context, "পিন সফলভাবে পরিবর্তন করা হয়েছে", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        errorMessage = "পিন পরিবর্তন ব্যর্থ হয়েছে"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText)
            ) {
                Text("পরিবর্তন করুন", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = if (allowDismiss) {
            {
                TextButton(onClick = onDismiss) {
                    Text("বাতিল", color = TextMuted)
                }
            }
        } else null,
        containerColor = DarkSurface,
        textContentColor = TextPrimary
    )
}

@Composable
fun FactoryResetConfirmDialog(
    allowDismiss: Boolean = true,
    onResetConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252)) },
        title = {
            Text(
                text = "⚠️ চূড়ান্ত সতর্কতা",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5252)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "আপনি কি নিশ্চিতভাবে ফোনটি ফ্যাক্টরি রিসেট করতে চান? আপনার ডিভাইসের সমস্ত ডাটা, ছবি ও অ্যাকাউন্ট মুছে যাবে এবং ফোন নতুনের মতো রিস্টার্ট হবে।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = "লক্ষ্য করুন: Focus Guard রিসেট করতে কোনো অতিরিক্ত পিনের প্রয়োজন নেই। চূড়ান্ত নিশ্চিত করতে নিচের বোতামটি চাপুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onResetConfirmed,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)
            ) {
                Text("হ্যাঁ, ফোন রিসেট করুন", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = if (allowDismiss) {
            {
                TextButton(onClick = onDismiss) {
                    Text("বাতিল", color = TextMuted)
                }
            }
        } else null,
        containerColor = DarkSurface,
        textContentColor = TextPrimary
    )
}

@Composable
fun SecurityPinAndResetCard(
    modifier: Modifier = Modifier,
    pinManager: SecurityPinManager? = null,
    dpcController: DevicePolicyController? = null
) {
    val context = LocalContext.current
    val actualPinManager = pinManager ?: remember { SecurityPinManager(context) }
    val actualDpcController = dpcController ?: remember { DevicePolicyController(context) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showFactoryResetDialog by remember { mutableStateOf(false) }

    if (showChangePinDialog) {
        ChangeSecurityPinDialog(
            pinManager = actualPinManager,
            onDismiss = { showChangePinDialog = false }
        )
    }

    if (showFactoryResetDialog) {
        FactoryResetConfirmDialog(
            onResetConfirmed = {
                showFactoryResetDialog = false
                val success = actualDpcController.wipeDevice()
                if (!success) {
                    Toast.makeText(context, "রিসেট ব্যর্থ হয়েছে। ডিভাইস ওনার পারমিশন নিশ্চিত করুন।", Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { showFactoryResetDialog = false }
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = GuardGreenText,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "সিকিউরিটি পিন ও রিসেট কন্ট্রোল",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Text(
                        text = "প্যারেন্টাল কন্ট্রোল দ্বারা রিসেট নিয়ন্ত্রণযোগ্য",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextMuted)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showChangePinDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GuardGreenText),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("পিন পরিবর্তন", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (!actualDpcController.isFactoryResetFeatureEnabled()) {
                            Toast.makeText(context, "প্যারেন্টাল কন্ট্রোল পলিসি অনুযায়ী রিসেট ফিচার বন্ধ রয়েছে।", Toast.LENGTH_SHORT).show()
                        } else {
                            showFactoryResetDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ফোন রিসেট", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 3-Dot Security Menu placed beside Focus Guard header.
 * Provides:
 * 1. প্যারেন্টাল কন্ট্রোল ও সিকিউরিটি সেটিংস (PIN protected)
 *    - প্যারেন্টাল কন্ট্রোল অ্যাক্সেস চালু/বন্ধ
 *    - প্যারেন্টাল কন্ট্রোল দ্বারা ফ্যাক্টরি রিসেট ফিচার চালু/বন্ধ
 * 2. ফোন ফ্যাক্টরি রিসেট (PIN মুক্ত, চূড়ান্ত সতর্কতা সহ সরাসরি রিসেট)
 * 3. পিন পরিবর্তন (Change PIN)
 */
@Composable
fun SecurityThreeDotMenu(
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    pinManager: SecurityPinManager? = null,
    dpcController: DevicePolicyController? = null
) {
    val context = LocalContext.current
    val actualPinManager = pinManager ?: remember { SecurityPinManager(context) }
    val actualDpcController = dpcController ?: remember { DevicePolicyController(context) }

    var showDropdownMenu by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showSecurityOptionsDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showFactoryResetDialog by remember { mutableStateOf(false) }

    if (showPinDialog) {
        EnterSecurityPinDialog(
            pinManager = actualPinManager,
            onPinVerified = {
                showPinDialog = false
                showSecurityOptionsDialog = true
            },
            onCancel = {
                showPinDialog = false
            }
        )
    }

    if (showSecurityOptionsDialog) {
        var isParentalBlocked by remember { mutableStateOf(actualDpcController.isParentalControlAccessBlocked()) }
        var isResetEnabled by remember { mutableStateOf(actualDpcController.isFactoryResetFeatureEnabled()) }

        AlertDialog(
            onDismissRequest = { showSecurityOptionsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = GuardGreenText,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "প্যারেন্টাল কন্ট্রোল সেটিংস",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "পিন যাচাই সফল হয়েছে। অভিভাবকীয় নিয়ন্ত্রণ ও রিসেট ফিচার পরিচালনা করুন:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )

                    // 1. Parental Control Access Block/Unblock
                    OutlinedButton(
                        onClick = {
                            val newTarget = !isParentalBlocked
                            val success = actualDpcController.setParentalControlAccessBlocked(newTarget)
                            if (success) {
                                isParentalBlocked = newTarget
                                val msg = if (newTarget) {
                                    "প্যারেন্টাল কন্ট্রোল অ্যাক্সেস বন্ধ করা হয়েছে (Action Not Allowed)।"
                                } else {
                                    "প্যারেন্টাল কন্ট্রোল অ্যাক্সেস চালু করা হয়েছে (Allowed)।"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "ডিভাইস ওনার অনুমতি প্রয়োজন।", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isParentalBlocked) Color(0xFFFFB74D) else GuardGreenText
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            if (isParentalBlocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                if (isParentalBlocked) "প্যারেন্টাল কন্ট্রোল আনব্লক করুন (Unblock)" else "প্যারেন্টাল কন্ট্রোল ব্লক করুন (Block Access)",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (isParentalBlocked) "স্ট্যাটাস: ব্লকড (Action Not Allowed) 🔒" else "স্ট্যাটাস: সচল (Allowed) 🔓",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isParentalBlocked) Color(0xFFFFB74D) else GuardGreenText
                            )
                        }
                    }

                    // 2. Factory Reset Feature Toggle via Parental Control
                    OutlinedButton(
                        onClick = {
                            val newTarget = !isResetEnabled
                            val success = actualDpcController.setFactoryResetFeatureEnabled(newTarget)
                            if (success) {
                                isResetEnabled = newTarget
                                val msg = if (newTarget) {
                                    "প্যারেন্টাল কন্ট্রোল: রিসেট ফিচার সক্রিয় করা হয়েছে।"
                                } else {
                                    "প্যারেন্টাল কন্ট্রোল: রিসেট ফিচার নিষ্ক্রিয় করা হয়েছে (Blocked)।"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "ডিভাইস ওনার অনুমতি প্রয়োজন।", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isResetEnabled) GuardGreenText else Color(0xFFFFB74D)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            if (isResetEnabled) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                if (isResetEnabled) "রিসেট ফিচার নিষ্ক্রিয় করুন (Disable Reset)" else "রিসেট ফিচার সক্রিয় করুন (Enable Reset)",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (isResetEnabled) "রিসেট স্ট্যাটাস: সচল (Allowed) 🔓" else "রিসেট স্ট্যাটাস: বন্ধ (Blocked by Policy) 🔒",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isResetEnabled) GuardGreenText else Color(0xFFFFB74D)
                            )
                        }
                    }

                    // 3. Change PIN
                    OutlinedButton(
                        onClick = {
                            showSecurityOptionsDialog = false
                            showChangePinDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GuardGreenText),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("পিন পরিবর্তন (Change PIN)", fontWeight = FontWeight.SemiBold)
                    }

                    // 4. Factory Reset (Confirmation only, zero PIN!)
                    Button(
                        onClick = {
                            showSecurityOptionsDialog = false
                            if (!isResetEnabled) {
                                Toast.makeText(context, "প্যারেন্টাল কন্ট্রোল পলিসি অনুযায়ী রিসেট ফিচার বন্ধ রয়েছে।", Toast.LENGTH_SHORT).show()
                            } else {
                                showFactoryResetDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ফোন ফ্যাক্টরি রিসেট (Factory Reset)", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSecurityOptionsDialog = false }) {
                    Text("বন্ধ করুন", color = TextMuted)
                }
            },
            containerColor = DarkSurface,
            textContentColor = TextPrimary
        )
    }

    if (showChangePinDialog) {
        ChangeSecurityPinDialog(
            pinManager = actualPinManager,
            onDismiss = { showChangePinDialog = false }
        )
    }

    if (showFactoryResetDialog) {
        FactoryResetConfirmDialog(
            onResetConfirmed = {
                showFactoryResetDialog = false
                val success = actualDpcController.wipeDevice()
                if (!success) {
                    Toast.makeText(context, "রিসেট ব্যর্থ হয়েছে। ডিভাইস ওনার পারমিশন নিশ্চিত করুন।", Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { showFactoryResetDialog = false }
        )
    }

    Box(modifier = modifier) {
        IconButton(onClick = { showDropdownMenu = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Security Options",
                tint = tint
            )
        }

        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false },
            modifier = Modifier.background(DarkSurface)
        ) {
            DropdownMenuItem(
                text = { Text("প্যারেন্টাল কন্ট্রোল ও সিকিউরিটি", color = TextPrimary) },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = GuardGreenText)
                },
                onClick = {
                    showDropdownMenu = false
                    showPinDialog = true
                }
            )

            DropdownMenuItem(
                text = { Text("ফোন ফ্যাক্টরি রিসেট", color = Color(0xFFFF5252), fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252))
                },
                onClick = {
                    showDropdownMenu = false
                    if (!actualDpcController.isFactoryResetFeatureEnabled()) {
                        Toast.makeText(context, "প্যারেন্টাল কন্ট্রোল পলিসি অনুযায়ী রিসেট ফিচার বন্ধ রয়েছে।", Toast.LENGTH_SHORT).show()
                    } else {
                        showFactoryResetDialog = true
                    }
                }
            )

            DropdownMenuItem(
                text = { Text("পিন পরিবর্তন", color = TextPrimary) },
                leadingIcon = {
                    Icon(Icons.Default.Password, contentDescription = null, tint = GuardGreenText)
                },
                onClick = {
                    showDropdownMenu = false
                    showChangePinDialog = true
                }
            )
        }
    }
}
