package au.gov.health.covidsafe.ui.settings

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.databinding.ActivityChangePostcodeBinding
import au.gov.health.covidsafe.links.LinkBuilder
import au.gov.health.covidsafe.ui.onboarding.fragment.personal.POST_CODE_REGEX
import kotlinx.android.synthetic.main.activity_change_postcode.*

class ChanePostCodeActivity: AppCompatActivity() {

    var viewModel: SettingsViewModel? = null
    var postcode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TO make sure scroll works with editTexts

        val binding = DataBindingUtil.setContentView<ActivityChangePostcodeBinding>(this, R.layout.activity_change_postcode)
        viewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        val toolbar = findViewById<View>(com.atlassian.mobilekit.module.feedback.R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        setupListener()

        viewModel?.getCurrentPostCode()
        postcode_content.text = LinkBuilder.getPostcodeContent(this)
        postcode_content.movementMethod = LinkMovementMethod.getInstance()

        postcode_updated.text = LinkBuilder.getPostcodeUpdatedSuccessfullyContent(this)
        postcode_updated.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupListener() {
        btn_done.setOnClickListener {
            this.finish()
        }
        btn_continue.setOnClickListener {
            postcode = post_code.text.toString()
            if (isValidPostcode() && postcode != null) {
                viewModel?.changePostcode(postcode!!)
                post_code_error.visibility = View.GONE
            } else {
                post_code_error.visibility = View.VISIBLE
            }
        }
    }
    private fun isValidPostcode() = postcode?.length == 4 && POST_CODE_REGEX.matcher(postcode).matches()
}