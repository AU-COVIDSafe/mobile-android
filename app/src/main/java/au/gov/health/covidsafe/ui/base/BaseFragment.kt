package au.gov.health.covidsafe.ui.base

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.Navigator
import androidx.navigation.fragment.NavHostFragment
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.ui.home.HomeFragmentDirections
import java.lang.Exception

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
        try {
            NavHostFragment.findNavController(this).navigate(actionId, bundle, null, navigatorExtras)
        } catch (e: Exception) {
            NavHostFragment.findNavController(this).navigateUp()
        }

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