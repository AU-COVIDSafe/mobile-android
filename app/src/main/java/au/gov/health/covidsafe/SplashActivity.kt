package au.gov.health.covidsafe

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import au.gov.health.covidsafe.ui.onboarding.OnboardingActivity
import au.gov.health.covidsafe.ui.splash.SplashNavigationEvent
import au.gov.health.covidsafe.ui.splash.SplashViewModel
import au.gov.health.covidsafe.ui.splash.SplashViewModelFactory
import kotlinx.android.synthetic.main.activity_splash.*
import java.util.*

class SplashActivity : AppCompatActivity() {

    private lateinit var viewModel: SplashViewModel

    private var retryProviderInstall: Boolean = false
    private val ERROR_DIALOG_REQUEST_CODE = 1

    private lateinit var mHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        hideSystemUI()
        mHandler = Handler()
        viewModel = ViewModelProvider(this, SplashViewModelFactory(this)).get(SplashViewModel::class.java)

        Preference.putDeviceID(this, Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID))

        viewModel.splashNavigationLiveData.observe(this, Observer {
            when (it) {
                is SplashNavigationEvent.GoToNextScreen -> goToNextScreen()
                is SplashNavigationEvent.ShowMigrationScreen -> migrationScreen()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        viewModel.setupUI()
    }

    override fun onPause() {
        super.onPause()
        mHandler.removeCallbacksAndMessages(null)
    }

    private fun migrationScreen() {
        splash_screen_logo.setImageResource(R.drawable.ic_logo_home_inactive)
        splash_screen_logo.setAnimation("spinner_migrating_db.json")
        splash_screen_logo.playAnimation()
        splash_migration_text.visibility = VISIBLE
        help_stop_covid.visibility = GONE
    }

    private fun goToNextScreen() {
        startActivity(Intent(this, if (!Preference.isOnBoarded(this)) {
            OnboardingActivity::class.java
        } else {
            HomeActivity::class.java
        }))
        viewModel.splashNavigationLiveData.removeObservers(this)
        viewModel.release()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ERROR_DIALOG_REQUEST_CODE) {
            retryProviderInstall = true
        }
    }

    // This snippet hides the system bars.
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

}

