package au.gov.health.covidsafe.ui.upload.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.talkback.setHeading
import au.gov.health.covidsafe.ui.base.PagerChildFragment
import au.gov.health.covidsafe.ui.base.UploadButtonLayout
import kotlinx.android.synthetic.main.fragment_upload_finished.*

class UploadFinishedFragment : PagerChildFragment() {

    override var navigationIconResId: Int? = null
    override var step: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_upload_finished, container, false)

    override fun onResume() {
        super.onResume()

        header.setHeading()
        header.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    override fun getUploadButtonLayout() = UploadButtonLayout.ContinueLayout(R.string.action_upload_done) {
        activity?.onBackPressed()
    }

    override fun updateButtonState() {
        enableContinueButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        root.removeAllViews()
    }
}