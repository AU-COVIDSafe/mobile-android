package au.gov.health.covidsafe.ui.splash

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import au.gov.health.covidsafe.streetpass.persistence.CURRENT_DB_VERSION
import au.gov.health.covidsafe.streetpass.persistence.MigrationCallBack
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecordDatabase
import kotlinx.coroutines.*

class SplashViewModel(context: Context) : ViewModel() {

    private val SPLASH_TIME: Long = 2000

    val splashNavigationLiveData = MutableLiveData<SplashNavigationEvent>(SplashNavigationEvent.ShowSplashScreen)

    private var migrated = false
    private var splashScreenPassed = false

    val db = StreetPassRecordDatabase.getDatabase(context, object : MigrationCallBack {
        override fun migrationStarted() {
            migrated = false
            if (splashScreenPassed) {
                viewModelScope.launch {
                    splashNavigationLiveData.value = SplashNavigationEvent.ShowMigrationScreen
                }
            }
        }

        override fun migrationFinished() {
            migrated = true
            if (splashScreenPassed) {
                viewModelScope.launch {
                    splashNavigationLiveData.value = SplashNavigationEvent.GoToNextScreen
                }
            }
        }
    })

    fun setupUI() {
        this.viewModelScope.launch {
            val splashScreenCoroutine = async(context = Dispatchers.IO) {
                delay(SPLASH_TIME)
                viewModelScope.launch {
                    if (migrated) {
                        splashNavigationLiveData.value = SplashNavigationEvent.GoToNextScreen
                    } else {
                        splashNavigationLiveData.value = SplashNavigationEvent.ShowMigrationScreen
                    }
                    splashScreenPassed = true
                }
            }
            val migratingCoroutine = async(context = Dispatchers.IO) {
                val readableDatabase = db.openHelper.readableDatabase
                migrated = !readableDatabase.needUpgrade(CURRENT_DB_VERSION)
                viewModelScope.launch {
                    if (migrated && splashScreenPassed) {
                        splashNavigationLiveData.value = SplashNavigationEvent.GoToNextScreen
                    } else if (!migrated) {
                        splashNavigationLiveData.value = SplashNavigationEvent.ShowMigrationScreen
                    }
                }
            }
            splashScreenCoroutine.join()
            migratingCoroutine.join()
        }
    }

    fun release() {
        StreetPassRecordDatabase.migrationCallback = null
        this.viewModelScope.cancel()
    }
}

class SplashViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = SplashViewModel(context) as T
}

sealed class SplashNavigationEvent {
    object ShowSplashScreen : SplashNavigationEvent()
    object ShowMigrationScreen : SplashNavigationEvent()
    object GoToNextScreen : SplashNavigationEvent()
}