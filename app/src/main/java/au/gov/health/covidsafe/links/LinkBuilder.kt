package au.gov.health.covidsafe.links

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.app.TracerApp
import au.gov.health.covidsafe.logging.CentralLog
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

const val TAG = "LinkBuilder"

private const val DEPARTMENT_OF_HEALTH_URL = "https://www.health.gov.au/"
private const val HOST_URL = "https://www.covidsafe.gov.au"
private const val CONTACT_US = "https://covidsafe.gov.au/privacy-policy.html#contact-us"
private const val HELP_TOPICS_BASE = "/help-topics"
private const val PRIVACY_TOPICS_BASE = "/privacy-policy"
private const val COLLECTION_NOTICE_BASE = "/collection-notice"

private const val TOPICS_EXT_SUFFIX = ".html"
private const val HELP_TOPICS_ENGLISH_PAGE = ""
private const val HELP_TOPICS_S_CHINESE_PAGE = "/zh-hans"
private const val HELP_TOPICS_T_CHINESE_PAGE = "/zh-hant"
private const val HELP_TOPICS_ARABIC_PAGE = "/ar"
private const val HELP_TOPICS_VIETNAMESE_PAGE = "/vi"
private const val HELP_TOPICS_KOREAN_PAGE = "/ko"
private const val HELP_TOPICS_GREEK_PAGE = "/el"
private const val HELP_TOPICS_ITALIAN_PAGE = "/it"
private const val HELP_TOPICS_PUNJABI_PAGE = "/pa-in"
private const val HELP_TOPICS_TURKISH_PAGE = "/tr"

private const val HELP_TOPICS_ANCHOR_VERIFY_MOBILE_NUMBER_PIN = "#verify-mobile-number-pin"
private const val HELP_TOPICS_ANCHOR_BLUETOOTH_PAIRING_REQUEST = "#bluetooth-pairing-request"
private const val HELP_TOPICS_ANCHOR_LOCATION_PERMISSION_ANDROID = "#location-permission-android"
private const val HELP_TOPICS_ANCHOR_READ_MORE = "#covidsafe-working-correctly"
private const val HELP_TOPICS_DELETE_INFORMATION = "#delete-information"

object LinkBuilder {

    private fun buildHtmlText(text: String) =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(text)
            }

    fun getHelpTopicsUrl(): String {
        val url = buildLocalisedURL(HELP_TOPICS_BASE)
        CentralLog.d(TAG, "getHelpTopicsUrl() $url")
        return url
    }

    fun getPrivacyTopicsUrl(): String {
        val url = buildLocalisedURL(PRIVACY_TOPICS_BASE)
        CentralLog.d(TAG, "getPrivacyTopicsUrl() $url")
        return url
    }

    private fun getCollectionNoticeUrl(): String {
        val url = buildLocalisedURL(COLLECTION_NOTICE_BASE)
        CentralLog.d(TAG, "getCollectionNoticeUrl() $url")
        return url
    }

    fun getCollectionMassageLearnMore(context: Context): SpannableString {
        return buildSpannableStringContent(
                context,
                TracerApp.AppContext.getString(R.string.collection_message),
                listOf(getPrivacyTopicsUrl())
        )
    }

    fun getLocationPermission(context: Context): SpannableString {
        return buildSpannableStringContent(
                context,
                TracerApp.AppContext.getString(R.string.update_screen_location),
                listOf(getLocationPairingRequestUrl())
        )
    }

    private fun buildLocalisedURL(path: String): String {
        val localeLanguageTag = Locale.getDefault().toLanguageTag()

        CentralLog.d(TAG, "Locale Language: $localeLanguageTag")

        return HOST_URL + path + when {
            localeLanguageTag.startsWith("zh-Hans") -> HELP_TOPICS_S_CHINESE_PAGE
            localeLanguageTag.startsWith("zh-Hant") -> HELP_TOPICS_T_CHINESE_PAGE
            localeLanguageTag.startsWith("ar") -> HELP_TOPICS_ARABIC_PAGE
            localeLanguageTag.startsWith("vi") -> HELP_TOPICS_VIETNAMESE_PAGE
            localeLanguageTag.startsWith("ko") -> HELP_TOPICS_KOREAN_PAGE
            localeLanguageTag.startsWith("el") -> HELP_TOPICS_GREEK_PAGE
            localeLanguageTag.startsWith("it") -> HELP_TOPICS_ITALIAN_PAGE
            localeLanguageTag.startsWith("pa") -> HELP_TOPICS_PUNJABI_PAGE
            localeLanguageTag.startsWith("tr") -> HELP_TOPICS_TURKISH_PAGE
            else -> HELP_TOPICS_ENGLISH_PAGE
        } + TOPICS_EXT_SUFFIX

    }

    fun getHelpTopicsUrlWithAnchor(anchor: String) =
            getHelpTopicsUrl() + anchor

    private fun getBluetoothPairingRequestUrl() =
            getHelpTopicsUrl() + HELP_TOPICS_ANCHOR_BLUETOOTH_PAIRING_REQUEST

    private fun getLocationPairingRequestUrl() =
            getHelpTopicsUrl() + HELP_TOPICS_ANCHOR_LOCATION_PERMISSION_ANDROID

    private fun getVerifyMobileNumberPinLink(linkText: String) = buildHtmlText(
            "<a href=\"${getHelpTopicsUrl() + HELP_TOPICS_ANCHOR_VERIFY_MOBILE_NUMBER_PIN}\">$linkText</a>")

    private fun getReadMoreLink(linkText: String) = buildHtmlText(
            "<a href=\"${getHelpTopicsUrl() + HELP_TOPICS_ANCHOR_READ_MORE}\">$linkText</a>")

    private fun getHotspotLink(linkText: String?, link: String) = buildHtmlText(
            "<a href=\"$link\">$linkText</a>")

    class LinkSpan(private val context: Context, private val linkURL: String) : ClickableSpan() {
        override fun onClick(widget: View) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkURL))
            context.startActivity(intent)
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)

            ds.isUnderlineText = true
//            ds.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
    }

    private fun buildSpannableStringContent(context: Context, originalString: String, links: List<String>): SpannableString {
        val stringBuilder = StringBuilder()
        val spanStartEndIndex = mutableListOf<Pair<Int, Int>>()

        val split = (" $originalString").split("*")

        split.forEachIndexed { index, s ->
            if (index % 2 == 1) {
                val start = stringBuilder.length - 1
                spanStartEndIndex.add(Pair(start, start + s.length))
            }

            stringBuilder.append(s)
        }

        val retVal = SpannableString(stringBuilder.toString().trim())

        spanStartEndIndex.forEachIndexed { index, pair ->
            try {
                retVal.setSpan(LinkSpan(context, links[index]), pair.first, pair.second, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } catch (e: Exception) {
                return retVal
            }
        }
        return retVal
    }

    fun getHowCOVIdSafeWorksContent(context: Context): SpannableString {
        return buildSpannableStringContent(
                context,
                TracerApp.AppContext.getString(R.string.how_it_works_content),
                listOf(getHelpTopicsUrl())
        )
    }

    fun getRegistrationAndPrivacyContent(context: Context): SpannableString {
        val privacyUrl = getPrivacyTopicsUrl()
        return buildSpannableStringContent(
                context,
                TracerApp.AppContext.getString(R.string.data_privacy_content_android),
                listOf(
                        privacyUrl,
                        privacyUrl,
                        getHelpTopicsUrl() + HELP_TOPICS_DELETE_INFORMATION,
                        HOST_URL,
                        privacyUrl,
                        CONTACT_US
                )
        )
    }

    fun getPostcodeContent(context: Context): SpannableString {
        val privacyUrl = getPrivacyTopicsUrl()
        return buildSpannableStringContent(
                context,
                TracerApp.AppContext.getString(R.string.change_postcode_intro),
                listOf(
                        privacyUrl,
                        getCollectionNoticeUrl()
                )
        )
    }

    fun getPostcodeUpdatedSuccessfullyContent(context: Context): SpannableString {
        val privacyUrl = getPrivacyTopicsUrl()
        return buildSpannableStringContent(
                context,
                TracerApp.AppContext.getString(R.string.permission_success_content),
                listOf(
                        privacyUrl,
                        getLocationPairingRequestUrl()
                )
        )
    }

    fun getHowPermissionSuccessContent(context: Context): SpannableString {
        return buildSpannableStringContent(
                context,
                TracerApp.AppContext.getString(R.string.permission_success_content),
                listOf(getBluetoothPairingRequestUrl(), getLocationPairingRequestUrl())
        )
    }

    fun getIssuesReceivingPINContent(): Spanned {
        val linkText = TracerApp.AppContext.getString(R.string.ReceivePinIssue)
        return getVerifyMobileNumberPinLink(linkText).also {
            CentralLog.d(TAG, "getIssuesReceivingPINContent() returns $it")
        }
    }

    fun getReadMore(): Spanned {
        val linkText = TracerApp.AppContext.getString(R.string.no_handshakes_link)
        return getReadMoreLink(linkText).also {
            CentralLog.d(TAG, "getReadMore returns $it")
        }
    }

    fun getHotSpot(title: String?, link: String): Spanned {
        return getHotspotLink(title,link).also {
            CentralLog.d(TAG, "getReadMore returns $it")
        }
    }

    fun getNoBluetoothPairingContent(context: Context): SpannableString {
        return buildSpannableStringContent(
                context,
                TracerApp.AppContext.getString(R.string.home_header_no_pairing),
                listOf(getBluetoothPairingRequestUrl())
        )
    }

    fun getUploadConsentContent(context: Context): SpannableString {
        return buildSpannableStringContent(
                context,
                TracerApp.AppContext.getString(R.string.upload_step_4_sub_header),
                listOf(getPrivacyTopicsUrl())
        )
    }
}