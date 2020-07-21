package au.gov.health.covidsafe.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.Navigator
import androidx.navigation.fragment.NavHostFragment
import au.gov.health.covidsafe.HasBlockingState
import kotlinx.android.synthetic.main.fragment_intro.*

open class BaseFragment : Fragment() {

    override fun onResume() {
        super.onResume()
        val activity = this.activity
        if (activity is HasBlockingState) {
            activity.isUiBlocked = false
        }
    }

    protected fun navigateTo(actionId: Int, bundle: Bundle? = null, navigatorExtras: Navigator.Extras? = null) {
        val activity = this.activity
        if (activity is HasBlockingState) {
            activity.isUiBlocked = true
        }
        NavHostFragment.findNavController(this).navigate(actionId, bundle, null, navigatorExtras)
    }

    protected fun popBackStack() {
        val activity = this.activity
        if (activity is HasBlockingState) {
            activity.isUiBlocked = true
        }
        NavHostFragment.findNavController(this).popBackStack()
    }

    protected fun removeViewInLandscapeMode(viewToRemove: View){
        if (requireContext().resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE){
            viewToRemove.visibility = View.GONE
        }
    }
}