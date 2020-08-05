package au.gov.health.covidsafe.ui.onboarding.fragment.permissionsuccess

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import au.gov.health.covidsafe.HomeActivity
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.links.LinkBuilder
import au.gov.health.covidsafe.talkback.setHeading
import au.gov.health.covidsafe.ui.PagerChildFragment
import au.gov.health.covidsafe.ui.UploadButtonLayout
import kotlinx.android.synthetic.main.fragment_permission_success.*

class PermissionSuccessFragment : PagerChildFragment() {

    override var step: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_permission_success, container, false)

    override fun onResume() {
        super.onResume()

        permission_success_headline.setHeading()
        permission_success_headline.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)

        permission_success_content.text = LinkBuilder.getHowPermissionSuccessContent(requireContext())
        permission_success_content.movementMethod = LinkMovementMethod.getInstance()

        disableNavigationButton()
    }

    private fun navigateToNextPage() {
        val intent = Intent(context, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity?.startActivity(intent)
        activity?.finish()
    }

    override fun getUploadButtonLayout() = UploadButtonLayout.ContinueLayout(R.string.permission_success_button) {
        navigateToNextPage()
    }

    override fun updateButtonState() {
        enableContinueButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        root.removeAllViews()
    }
}