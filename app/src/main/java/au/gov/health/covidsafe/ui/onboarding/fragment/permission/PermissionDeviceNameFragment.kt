package au.gov.health.covidsafe.ui.onboarding.fragment.permission

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.talkback.setHeading
import au.gov.health.covidsafe.ui.PagerChildFragment
import au.gov.health.covidsafe.ui.UploadButtonLayout
import kotlinx.android.synthetic.main.fragment_permission.root
import kotlinx.android.synthetic.main.fragment_permission_device_name.*

class PermissionDeviceNameFragment : PagerChildFragment() {

    override var stepProgress: Int? = 5

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_permission_device_name, container, false)

    override fun onResume() {
        super.onResume()

        context?.let {
            val deviceName = "<b>${BluetoothAdapter.getDefaultAdapter()?.name}</b>"

            val paragraph1 = it.getString(R.string.change_device_name_content_line_1, deviceName)
            val paragraph2 = "<br/><br/>" + it.getString(R.string.change_device_name_content_line_2)

            val paragraphs = "$paragraph1$paragraph2"

            change_device_name_content.text =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        Html.fromHtml(paragraphs, Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        Html.fromHtml(paragraphs)
                    }
        }


        change_device_name_headline.setHeading()
        change_device_name_headline.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    private fun navigateToNextPage() {
        navigateTo(R.id.action_permissionDeviceNameFragment_to_permissionSuccessFragment)
    }

    override fun getUploadButtonLayout() = UploadButtonLayout.TwoChoiceContinueLayout(
            R.string.change_device_name_primary_action,
            {
                BluetoothAdapter.getDefaultAdapter()?.name = change_device_name_text_box.text.toString()
                navigateToNextPage()
            },
            R.string.change_device_name_secondary_action,
            {
                navigateToNextPage()
            }
    )

    override fun updateButtonState() {
        enableContinueButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        root.removeAllViews()
    }
}