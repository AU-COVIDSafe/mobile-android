package au.gov.health.covidsafe.extensions

import android.Manifest.permission.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.ui.utils.Utils
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest


const val REQUEST_ENABLE_BT = 123
const val LOCATION = 345
const val BATTERY_OPTIMISER = 789

fun Fragment.requestAllPermissions(onEndCallback: () -> Unit) {
    if (requireContext().isBlueToothEnabled() ?: true) {
        requestFineLocationAndCheckBleSupportThenNextPermission(onEndCallback)
    } else {
        requestBlueToothPermissionThenNextPermission()
    }
}

fun Fragment.requestBlueToothPermissionThenNextPermission() {
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
}

fun Fragment.checkBLESupport() {
    if (BluetoothAdapter.getDefaultAdapter()?.isMultipleAdvertisementSupported?.not() ?: false) {
        activity?.let {
            Utils.stopBluetoothMonitoringService(it)
        }
    }
}

private fun Fragment.requestFineLocationAndCheckBleSupportThenNextPermission(onEndCallback: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        activity?.let {
            when {
                EasyPermissions.hasPermissions(it, ACCESS_FINE_LOCATION) -> {
                    checkBLESupport()
                    excludeFromBatteryOptimization(onEndCallback)
                }
                else -> {
                    EasyPermissions.requestPermissions(
                            PermissionRequest.Builder(this, LOCATION, ACCESS_FINE_LOCATION)
                                    .setRationale(R.string.permission_location_rationale)
                                    .build())
                }
            }
        }
    } else {
        checkBLESupport()
        onEndCallback.invoke()
    }
}

fun Fragment.excludeFromBatteryOptimization(onEndCallback: (() -> Unit)? = null) {
    activity?.let {
        val powerManager =
                it.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
        val packageName = it.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Utils.getBatteryOptimizerExemptionIntent(packageName)
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                //check if there's any activity that can handle this
                if (Utils.canHandleIntent(intent, it.packageManager)) {
                    this.startActivityForResult(intent, BATTERY_OPTIMISER)
                } else {
                    //no way of handling battery optimizer
                    onEndCallback?.invoke()
                }
            } else {
                onEndCallback?.invoke()
            }
        }
    }

}

fun Fragment.gotoPushNotificationSettings() {
    val context = requireContext()
    val intent = Intent()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("app_package", context.packageName)
        intent.putExtra("app_uid", context.applicationInfo.uid)
    }

    context.startActivity(intent)
}

fun Context.isBlueToothEnabled(): Boolean? {
    val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
    return bluetoothManager?.adapter?.isEnabled
}

fun Context.isLocationPermissionAllowed(): Boolean? {
    return EasyPermissions.hasPermissions(this, ACCESS_FINE_LOCATION)
}

fun Context.isLocationEnabledOnDevice(): Boolean {
    val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
    return locationManager?.let {
        it.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                it.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } ?: false
}

fun Context.isBatteryOptimizationDisabled(): Boolean? {
    val powerManager = this.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager?
    val packageName = this.packageName

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return powerManager?.isIgnoringBatteryOptimizations(packageName) ?: true
    }

    return null
}

fun Fragment.askForLocationPermission() {
    activity?.let {
        when {
            !EasyPermissions.hasPermissions(it, ACCESS_FINE_LOCATION) -> {
                EasyPermissions.requestPermissions(
                        PermissionRequest.Builder(this, LOCATION, ACCESS_FINE_LOCATION)
                                .setRationale(R.string.permission_location_rationale)
                                .build())
            }

            !it.isLocationEnabledOnDevice() -> {
                AlertDialog.Builder(it).apply {
                    setMessage(R.string.need_location_service)
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        it.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                }.create().show()
            }

            else -> {
            }// do nothing
        }
    }
}
