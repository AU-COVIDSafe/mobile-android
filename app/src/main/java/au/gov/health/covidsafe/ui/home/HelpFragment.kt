package au.gov.health.covidsafe.ui.home

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.atlassian.mobilekit.module.feedback.FeedbackModule
import kotlinx.android.synthetic.main.fragment_help.*
import kotlinx.android.synthetic.main.fragment_help.view.*
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.ui.BaseFragment
import kotlinx.android.synthetic.main.activity_country_code_selection.*
import java.util.*

private const val HELP_URL_BASE = "https://www.covidsafe.gov.au/help-topics"
private const val HELP_URL_ENGLISH_PAGE = ".html"
private const val HELP_URL_ARABIC_PAGE = "/ar.html"
private const val HELP_URL_VIETNAMESE_PAGE = "/vi.html"
private const val HELP_URL_KOREAN_PAGE = "/ko.html"
private const val HELP_URL_SIMPLIFIED_CHINESE_PAGE = "/zh-hans.html"
private const val HELP_URL_TRADITIONAL_CHINESE_PAGE = "/zh-hant.html"

private const val TAG = "HelpFragment"

class HelpFragment : BaseFragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_help, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val webView = view.helpWebView
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = createWebVieClient(view)
        webView.loadUrl(getHelpUrlBasedOnLocaleLanguage())
        reportAnIssue.setOnClickListener {
            FeedbackModule.showFeedbackScreen()
        }
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            toolbar.navigationIcon =
                    requireContext().getDrawable(R.drawable.ic_up_rtl)
        }
    }

    private fun createWebVieClient(view: View): WebViewClient =
            object : WebViewClient() {
                private var isRedirecting = false
                private var loadFinished = false

                override fun shouldOverrideUrlLoading(webView: WebView, request: WebResourceRequest): Boolean {
                    if (!loadFinished) isRedirecting = true
                    loadFinished = false
                    val urlString = request.url.toString()
                    if (urlString.startsWith(HELP_URL_BASE)) {
                        webView.loadUrl(request.url.toString())
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
                        webView.context.startActivity(intent)
                    }
                    return true
                }

                override fun onPageStarted(webView: WebView, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(webView, url, favicon)
                    loadFinished = false
                    view.progress.isVisible = true
                }

                override fun onPageFinished(webView: WebView, url: String?) {
                    super.onPageFinished(webView, url)

                    if (!isRedirecting) loadFinished = true

                    if (loadFinished && !isRedirecting) {
                        view.progress.isVisible = false
                    } else {
                        isRedirecting = false
                    }
                }
            }


    private fun getHelpUrlBasedOnLocaleLanguage(): String {
        val localeLanguageTag = Locale.getDefault().toLanguageTag()

        val url = HELP_URL_BASE + when {
            localeLanguageTag.startsWith("zh-Hans") -> HELP_URL_SIMPLIFIED_CHINESE_PAGE
            localeLanguageTag.startsWith("zh-Hant") -> HELP_URL_TRADITIONAL_CHINESE_PAGE
            localeLanguageTag.startsWith("ar") -> HELP_URL_ARABIC_PAGE
            localeLanguageTag.startsWith("vi") -> HELP_URL_VIETNAMESE_PAGE
            localeLanguageTag.startsWith("ko") -> HELP_URL_KOREAN_PAGE
            else -> HELP_URL_ENGLISH_PAGE
        }


        CentralLog.d(TAG, "getHelpUrlBasedOnLocaleLanguage() " +
                "localeLanguageTag = $localeLanguageTag " +
                "url = $url")

        return url
    }
}


