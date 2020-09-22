package au.gov.health.covidsafe.ui.base

import androidx.annotation.StringRes

interface PagerContainer {
    fun enableNextButton()
    fun disableNextButton()
    fun showLoading()
    fun hideLoading(@StringRes stringRes: Int?)
    fun updateSteps(step: Int?, totalSteps: Int)
    fun setNavigationIcon(navigationIcon: Int?)
    fun refreshButton(updateButtonLayout: UploadButtonLayout)
}