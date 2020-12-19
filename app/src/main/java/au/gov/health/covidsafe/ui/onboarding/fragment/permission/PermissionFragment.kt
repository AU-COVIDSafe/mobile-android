package au.gov.health.covidsafe.ui.onboarding.fragment.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import au.gov.health.covidsafe.HomeActivity
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.app.TracerApp
import au.gov.health.covidsafe.extensions.*
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.talkback.setHeading
import au.gov.health.covidsafe.ui.base.PagerChildFragment
import au.gov.health.covidsafe.ui.base.UploadButtonLayout
import kotlinx.android.synthetic.main.fragment_permission.*
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import kotlin.collections.ArrayList

class PermissionFragment : PagerChildFragment(), EasyPermissions.PermissionCallbacks {

    companion object {
        val requiredPermissions = PermissionFragment().requestPermissions()
    }

    override var step: Int? = 4

    private var navigationStarted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_permission, container, false)

    override fun onResume() {
        super.onResume()

        removeViewInLandscapeMode(permission_picture)

        permission_headline.setHeading()
        permission_headline.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)

        disableNavigationButton()

        activity?.let {
            Preference.putIsOnBoarded(it, true)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                excludeFromBatteryOptimization { navigateToNextPage() }
                return
            } else {
                requestAllPermissions { navigateToNextPage() }
            }
        } else if (requestCode == BATTERY_OPTIMISER) {
            Handler().postDelayed({
                navigateToNextPage()
            }, 1000)
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private fun requestPermissions():  MutableList<String> {
        // Check and request permissions
        val requiredPermissions: MutableList<String> = ArrayList()
        requiredPermissions.add(Manifest.permission.BLUETOOTH)
        requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }
        requiredPermissions.add(Manifest.permission.WAKE_LOCK)

        return requiredPermissions
    }

    private fun navigateToNextPage() {
        navigationStarted = false
        if (hasAllPermissionsAndBluetoothOn()) {
            navigateTo(R.id.action_permissionFragment_to_permissionDeviceNameFragment)
        } else {
            navigateToMainActivity()
        }
    }

    private fun hasAllPermissionsAndBluetoothOn(): Boolean {
        val context = TracerApp.AppContext
        return context.isBlueToothEnabled() == true && checkAllRequiredPermissions() && checkIgnoreBatteryOptimization()
    }

    private fun checkAllRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val context = TracerApp.AppContext
            requiredPermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        } else {
            true
        }
    }

    private fun checkIgnoreBatteryOptimization(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val context = TracerApp.AppContext
            ContextCompat.getSystemService(context, PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } else {
            true
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(context, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity?.startActivity(intent)
        activity?.finish()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == LOCATION) {
            excludeFromBatteryOptimization { navigateToNextPage() }
        } else {
            requestAllPermissions { navigateToNextPage() }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        requestAllPermissions { navigateToNextPage() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun getUploadButtonLayout() = UploadButtonLayout.ContinueLayout(R.string.permission_button) {
        disableContinueButton()
        navigationStarted = true
        requestAllPermissions {
            navigateToNextPage()
        }
    }

    override fun updateButtonState() {
        if (navigationStarted) {
            disableContinueButton()
        } else {
            enableContinueButton()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        root.removeAllViews()
    }
}