package au.gov.health.covidsafe.ui.onboarding.fragment.howitworks

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
import kotlinx.android.synthetic.main.fragment_how_it_works.*
import kotlinx.android.synthetic.main.fragment_how_it_works.view.*

class HowItWorksFragment : PagerChildFragment() {

    override var step: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_how_it_works, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.how_it_works_content.text = LinkBuilder.getHowCOVIdSafeWorksContent(requireContext())
        view.how_it_works_content.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onResume() {
        super.onResume()

        removeViewInLandscapeMode(how_it_works_picture)

        how_it_works_headline.setHeading()
        how_it_works_headline.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    override fun getUploadButtonLayout() = UploadButtonLayout.ContinueLayout(R.string.how_it_works_button) {
        navigateTo(R.id.action_howItWorksFragment_to_dataPrivacy)
    }

    override fun updateButtonState() {
        enableContinueButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        root.removeAllViews()
    }
}