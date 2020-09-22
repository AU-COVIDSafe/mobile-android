package au.gov.health.covidsafe.ui.onboarding.fragment.dataprivacy

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.links.LinkBuilder
import au.gov.health.covidsafe.talkback.setHeading
import au.gov.health.covidsafe.ui.base.PagerChildFragment
import au.gov.health.covidsafe.ui.base.UploadButtonLayout
import kotlinx.android.synthetic.main.fragment_data_privacy.*
import kotlinx.android.synthetic.main.fragment_data_privacy.root
import kotlinx.android.synthetic.main.fragment_data_privacy.view.*

class DataPrivacyFragment : PagerChildFragment() {

    override var step: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_data_privacy, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.data_privacy_content.text = LinkBuilder.getRegistrationAndPrivacyContent(requireContext())
        view.data_privacy_content.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onResume() {
        super.onResume()

        removeViewInLandscapeMode(data_privacy_picture)

        data_privacy_headline.setHeading()
        data_privacy_headline.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    override fun getUploadButtonLayout() = UploadButtonLayout.ContinueLayout(R.string.data_privacy_button) {
        navigateTo(DataPrivacyFragmentDirections.actionDataPrivacyToRegistrationConsentFragment().actionId)
    }

    override fun updateButtonState() {
        enableContinueButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        root.removeAllViews()
    }
}