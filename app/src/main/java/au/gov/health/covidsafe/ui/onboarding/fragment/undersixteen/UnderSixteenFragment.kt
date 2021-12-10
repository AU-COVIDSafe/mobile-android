package au.gov.health.covidsafe.ui.onboarding.fragment.undersixteen

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.talkback.setHeading
import au.gov.health.covidsafe.ui.base.PagerChildFragment
import au.gov.health.covidsafe.ui.base.UploadButtonLayout
import au.gov.health.covidsafe.ui.onboarding.fragment.enternumber.EnterNumberFragment
import kotlinx.android.synthetic.main.activity_onboarding.*
import kotlinx.android.synthetic.main.fragment_under_sixteen.*

class UnderSixteenFragment : PagerChildFragment() {

    override var step: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_under_sixteen, container, false)

    override fun onResume() {
        super.onResume()
        updateButtonState()

        under_sixteen_headline.setHeading()
        under_sixteen_headline.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)

        val content = this.getString(R.string.under_sixteen_content).replace("Privacy Act 1988", "<i>Privacy Act 1988</i>")
        under_sixteen_text.text = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_LEGACY)
        enableConsentButton()
    }

    override fun getUploadButtonLayout(): UploadButtonLayout = UploadButtonLayout.ContinueLayout(R.string.consent_button) {
        val bundle = bundleOf(
                EnterNumberFragment.ENTER_NUMBER_DESTINATION_ID to R.id.action_otpFragment_to_permissionFragment,
                EnterNumberFragment.ENTER_NUMBER_PROGRESS to 2)
        navigateTo(UnderSixteenFragmentDirections.actionUnderSixteenFragmentToEnterNumberFragment().actionId, bundle)
    }

    override fun updateButtonState() {
    }

    override fun onPause() {
        super.onPause()
        invisibleConsentButton()
    }
}