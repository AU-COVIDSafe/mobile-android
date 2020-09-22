package au.gov.health.covidsafe.ui.base

import android.view.View
import androidx.annotation.StringRes

abstract class PagerChildFragment : BaseFragment() {
    override fun onResume() {
        super.onResume()
        updateToolBar()
        updateButton()
        updateProgressBar()
        updateButtonState()
    }

    private fun updateProgressBar() {
        (parentFragment?.parentFragment as? PagerContainer)?.updateSteps(step, totalSteps)
        (activity as? PagerContainer)?.updateSteps(step, totalSteps)
    }

    private fun updateToolBar() {
        if (navigationIconResId == au.gov.health.covidsafe.R.drawable.ic_up) {
            if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL){
                navigationIconResId = au.gov.health.covidsafe.R.drawable.ic_up_rtl
            }
        }

        (parentFragment?.parentFragment as? PagerContainer)?.setNavigationIcon(navigationIconResId)
        (activity as? PagerContainer)?.setNavigationIcon(navigationIconResId)
    }

    private fun updateButton() {
        val updateButtonLayout = getUploadButtonLayout()
        if (updateButtonLayout is UploadButtonLayout.ContinueLayout) {
            updateButtonState()
        }
        (parentFragment?.parentFragment as? PagerContainer)?.refreshButton(updateButtonLayout)
        (activity as? PagerContainer)?.refreshButton(updateButtonLayout)
    }

    fun disableNavigationButton(){
        (parentFragment?.parentFragment as? PagerContainer)?.setNavigationIcon(null)
        (activity as? PagerContainer)?.setNavigationIcon(null)
    }

    fun enableContinueButton() {
        (parentFragment?.parentFragment as? PagerContainer)?.enableNextButton()
        (activity as? PagerContainer)?.enableNextButton()
    }

    fun disableContinueButton() {
        (parentFragment?.parentFragment as? PagerContainer)?.disableNextButton()
        (activity as? PagerContainer)?.disableNextButton()
    }

    fun showLoading() {
        (parentFragment?.parentFragment as? PagerContainer)?.showLoading()
        (activity as? PagerContainer)?.showLoading()
    }

    fun hideLoading() {
        (parentFragment?.parentFragment as? PagerContainer)?.hideLoading((getUploadButtonLayout() as? UploadButtonLayout.ContinueLayout)?.buttonText)
        (activity as? PagerContainer)?.hideLoading((getUploadButtonLayout() as? UploadButtonLayout.ContinueLayout)?.buttonText)
    }

    protected open var navigationIconResId: Int? = au.gov.health.covidsafe.R.drawable.ic_up

    abstract var step: Int?
    private val totalSteps = 4

    abstract fun getUploadButtonLayout(): UploadButtonLayout
    abstract fun updateButtonState()
}

sealed class UploadButtonLayout {
    class ContinueLayout(@StringRes val buttonText: Int, val buttonListener: (() -> Unit)?) : UploadButtonLayout()

    class TwoChoiceContinueLayout(
            @StringRes val primaryButtonText: Int,
            val primaryButtonListener: (() -> Unit)?,
            @StringRes val secondaryButtonText: Int,
            val secondaryButtonListener: (() -> Unit)?
    ) : UploadButtonLayout()

    class QuestionLayout(val buttonYesListener: () -> Unit, val buttonNoListener: () -> Unit) : UploadButtonLayout()
}

