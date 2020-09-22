package au.gov.health.covidsafe.ui.connection

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import au.gov.health.covidsafe.R
import kotlinx.android.synthetic.main.activity_internet_connection_issues.*
import kotlinx.android.synthetic.main.activity_onboarding.toolbar

private const val TAG = "InternetConnectionIssuesActivity"

class InternetConnectionIssuesActivity : FragmentActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_internet_connection_issues)

        text_internet_content_1.text = "\u2022 ${getString(R.string.internet_screen_content_1)}"
        text_internet_content_2.text = "\u2022 ${getString(R.string.internet_screen_content_2)}"

        toolbar.setNavigationOnClickListener {
            super.onBackPressed()
        }
    }
}