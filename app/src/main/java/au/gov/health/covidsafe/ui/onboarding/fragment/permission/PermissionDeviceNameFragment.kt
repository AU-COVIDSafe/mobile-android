package au.gov.health.covidsafe.ui.onboarding.fragment.permission

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import au.gov.health.covidsafe.Preference
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.talkback.setHeading
import au.gov.health.covidsafe.ui.PagerChildFragment
import au.gov.health.covidsafe.ui.UploadButtonLayout
import kotlinx.android.synthetic.main.fragment_permission.root
import kotlinx.android.synthetic.main.fragment_permission_device_name.*

class PermissionDeviceNameFragment : PagerChildFragment() {

    override var step: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_permission_device_name, container, false)

    override fun onResume() {
        super.onResume()

        context?.let {
            change_device_name_content.setText(R.string.change_device_name_content_line_2)
            change_device_name_text_box.setText(Settings.Secure.getString(it.contentResolver, "bluetooth_name"))
            Preference.setDeviceNameChangePromptDisplayed(it)
        }

        change_device_name_headline.setHeading()
        change_device_name_headline.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)

        disableNavigationButton()
    }

    private fun navigateToNextPage() {
        navigateTo(R.id.action_permissionDeviceNameFragment_to_permissionSuccessFragment)
    }

    override fun getUploadButtonLayout() = UploadButtonLayout.ContinueLayout(
            R.string.change_device_name_primary_action
    ) {
        BluetoothAdapter.getDefaultAdapter()?.name = change_device_name_text_box.text.toString()
        navigateToNextPage()
    }

    override fun updateButtonState() {
        enableContinueButton()
        disableNavigationButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        root.removeAllViews()
    }
}