package au.gov.health.covidsafe.ui.upload.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.talkback.setHeading
import au.gov.health.covidsafe.ui.PagerChildFragment
import au.gov.health.covidsafe.ui.UploadButtonLayout
import kotlinx.android.synthetic.main.fragment_upload_initial.*
import kotlinx.android.synthetic.main.fragment_upload_page_4.root

class UploadInitialFragment : PagerChildFragment() {

    override var stepProgress: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_upload_initial, container, false)


    override fun onResume() {
        super.onResume()

        removeViewInLandscapeMode(upload_initial_picture)

        upload_initial_headline.setHeading()
        upload_initial_headline.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    override fun updateButtonState() {
        enableContinueButton()
    }

    override fun getUploadButtonLayout() = UploadButtonLayout.QuestionLayout(
            buttonYesListener = {
                navigateTo(R.id.action_uploadInitial_to_uploadStepFourFragment)
            },
            buttonNoListener = {
                activity?.onBackPressed()
            })

    override fun onDestroyView() {
        super.onDestroyView()
        root.removeAllViews()
    }

}
