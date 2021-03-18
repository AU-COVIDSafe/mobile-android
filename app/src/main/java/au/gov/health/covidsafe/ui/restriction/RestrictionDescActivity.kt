package au.gov.health.covidsafe.ui.restriction

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import au.gov.health.covidsafe.R
import kotlinx.android.synthetic.main.activity_restriction_desc.*

class RestrictionDescActivity : AppCompatActivity() {

    var htmlText: String = ""
    var title:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TO make sure scroll works with editTexts
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_restriction_desc)
        val toolbar = findViewById<View>(R.id.toolbar_restriction_desc) as Toolbar
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val extras = intent.extras
        extras?.let {
            val toolbarTitle = intent.extras?.getString("toolbarTitle","")
            val titleSplit = toolbarTitle?.split(" ")
            if (titleSplit != null && titleSplit.size > 5) {
                supportActionBar?.setTitle("").toString()
                txt_toolbar_title2.text = toolbarTitle.toString()
            } else {
                supportActionBar?.setTitle(intent.extras?.getString("toolbarTitle","")).toString()
                txt_toolbar_title2.visibility = View.GONE
            }
            txt_activity.text = intent.extras?.getString("ActivityTitle","").toString()
            htmlText = intent.extras?.getString("htmlDesc","0").toString()
        }

        val summary = "<html><head><style>a{color:#00661B}</style></head>" +
                "<body >$htmlText</body></html>"

        web_view.loadDataWithBaseURL(null, summary, "text/html", "utf-8", null)
    }
}