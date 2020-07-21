package au.gov.health.covidsafe.ui.onboarding.fragment.personal

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSpinner

class SelectionCallbackSpinner : AppCompatSpinner {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    interface CustomSpinnerSelectionCallback {
        fun onItemSelected(position: Int)
    }

    private var selectionCallback: CustomSpinnerSelectionCallback? = null

    fun setSelectionCallback(selectionCallback: CustomSpinnerSelectionCallback) {
        this.selectionCallback = selectionCallback
    }

    fun removeSelectionCallback() {
        selectionCallback = null
    }

    override fun setSelection(position: Int) {
        super.setSelection(position)

        if (position == selectedItemPosition) selectionCallback?.onItemSelected(position)
    }
}