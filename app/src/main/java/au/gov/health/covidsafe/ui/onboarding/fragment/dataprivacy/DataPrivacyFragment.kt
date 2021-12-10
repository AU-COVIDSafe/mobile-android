package au.gov.health.covidsafe.ui.onboarding.fragment.dataprivacy

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.StyleSpan
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
import kotlinx.android.synthetic.main.fragment_data_privacy.view.*

class DataPrivacyFragment : PagerChildFragment() {

    override var step: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_data_privacy, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val content = LinkBuilder.getRegistrationAndPrivacyContent(requireContext())
        val privacy = "Privacy Act 1988"
        val startIndex: Int = content.indexOf(privacy)

        content.setSpan(
                StyleSpan(Typeface.ITALIC),
                startIndex, (startIndex + privacy.length + 1),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        view.data_privacy_content.text = content
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