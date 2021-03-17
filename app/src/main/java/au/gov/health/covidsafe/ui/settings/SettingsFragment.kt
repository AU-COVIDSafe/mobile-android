package au.gov.health.covidsafe.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import au.gov.health.covidsafe.HomeActivity
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.extensions.allPermissionsEnabled
import au.gov.health.covidsafe.extensions.getAppVersionNumberDetails
import au.gov.health.covidsafe.extensions.shareThisApp
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.response.MessagesResponse
import au.gov.health.covidsafe.ui.base.BaseFragment
import au.gov.health.covidsafe.ui.connection.InternetConnectionIssuesActivity
import au.gov.health.covidsafe.ui.home.HelpActivity
import au.gov.health.covidsafe.ui.home.HelpFragment
import au.gov.health.covidsafe.ui.home.view.ExternalLinkCard
import au.gov.health.covidsafe.utils.NetworkConnectionCheck
import com.atlassian.mobilekit.module.feedback.FeedbackModule
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.fragment_settings_external_link.*
import kotlinx.android.synthetic.main.view_covid_share_tile.*
import kotlinx.android.synthetic.main.view_help_topics_tile.*
import kotlinx.android.synthetic.main.view_settings_improve_app_performance_tile.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val TAG = "SettingsFragment"

class SettingsFragment : BaseFragment(), NetworkConnectionCheck.NetworkConnectionListener {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeToolbarNavigation()
        initializeObservers()
        NetworkConnectionCheck.addNetworkChangedListener(requireContext(), this)
    }

    private fun initializeToolbarNavigation() {
        settings_toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun initializeObservers() {
        (activity as HomeActivity?)?.run {
            appUpdateAvailableMessageResponseLiveData.observe(viewLifecycleOwner, latestAppAvailable)
        }
    }

    override fun onResume() {
        super.onResume()

        updateNotificationStatusTile()
        initializeHelpTopicsNavigation()
        initializeSupportNavigation()
        initializeAppShareNavigation()
        setAppVersionNumber()
        initializeChangePostCodeNavigation()
    }

    private fun initializeHelpTopicsNavigation() {
        help_topics_link.setOnClickListener {
            HelpFragment.anchor = null
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }
    }

    private fun initializeSupportNavigation() {
        settings_support_view.setOnClickListener {
            FeedbackModule.showFeedbackScreen()
        }
    }

    private fun initializeAppShareNavigation() {
        app_share.setOnClickListener {
            context?.shareThisApp()
        }
    }

    private fun initializeChangePostCodeNavigation() {
        postcode_card_view.setOnClickListener {
            startActivity(Intent(requireContext(), ChanePostCodeActivity::class.java))
        }
    }

    private fun setAppVersionNumber() {
        settings_version_number.text = context?.getAppVersionNumberDetails()
    }

    private val latestAppAvailable = Observer<MessagesResponse> {
        updateMessageTiles(it)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        NetworkConnectionCheck.removeNetworkChangedListener(this)
        removeObservers()
    }

    private fun removeObservers() {
        (activity as HomeActivity?)?.run {
            appUpdateAvailableMessageResponseLiveData.removeObserver(latestAppAvailable)
        }
    }

    private fun updateNotificationStatusTile() {
        val title: String
        val body: String

        if (NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
            notification_status_link.setTopRightIcon(R.drawable.ic_green_tick)
            title = getString(R.string.home_set_complete_external_link_notifications_title_iOS)
            body = getString(R.string.NotificationsEnabledBlurb)
        } else {
            notification_status_link.setTopRightIcon(R.drawable.ic_white_background_red_error)
            title = getString(R.string.home_set_complete_external_link_notifications_title_iOS_off)
            body = getString(R.string.NotificationsEnabledBlurb)
        }

        notification_status_link.setTitleBodyAndClickCallback(title, body) {
            openAppNotificationSettings()
        }

        notification_status_link.setColorForContentWithAction()
    }

    private fun updateInternetConnectionTile(isInternetConnected: Boolean) {
        // called on IO thread; run the UI logic on UI thread
        GlobalScope.launch(Dispatchers.Main) {
            context?.let {
                CentralLog.d(TAG, "updateConnectionTile() isInternetConnected = $isInternetConnected")

                var visibility = if (isInternetConnected) View.GONE else View.VISIBLE

                // don't display the tile when there's permission not enabled
                val isAllPermissionsEnabled = it.allPermissionsEnabled()
                if (!isAllPermissionsEnabled) {
                    visibility = View.GONE
                }

                improve_performance_group.visibility = visibility
                internet_connection_tile.visibility = visibility

                if (visibility == View.VISIBLE) {
                    internet_connection_tile.setOnClickListener {
                        startActivity(Intent(requireContext(), InternetConnectionIssuesActivity::class.java))
                    }
                }
            }
        }
    }

    private fun updateMessageTiles(messagesResponse: MessagesResponse) {
        GlobalScope.launch(Dispatchers.Main) {
            improve_performance_card_linear_layout.children.forEach {
                if (it != internet_connection_tile && it != improve_performance_title) {
                    improve_performance_card_linear_layout.removeView(it)
                }
            }

            if (!messagesResponse.messages.isNullOrEmpty()) {
                improve_performance_group.visibility = View.VISIBLE

                messagesResponse.messages.forEach { message ->
                    ExternalLinkCard(requireContext(), null, 0).also {
                        it.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { layoutParams ->
                            layoutParams.setMargins(0, resources.getDimensionPixelSize(R.dimen.divider_height), 0, 0)
                        }

                        it.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))

                        it.setMessage(message)
                        it.setErrorTextColor()

                        improve_performance_card_linear_layout.addView(it)
                    }
                }
            } else {
                improve_performance_group.visibility = View.GONE
            }
        }
    }

    private fun openAppNotificationSettings() {
        val context = requireContext()

        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", context.packageName)
                    putExtra("app_uid", context.applicationInfo.uid)
                }
                else -> {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    addCategory(Intent.CATEGORY_DEFAULT)
                    data = Uri.parse("package:" + context.packageName)
                }
            }
        }

        context.startActivity(intent)
    }

    override fun onNetworkStatusChanged(isAvailable: Boolean) {
        CentralLog.d(TAG, "Settings onNetworkStatusChanged: $isAvailable")
        updateInternetConnectionTile(isAvailable)
    }
}