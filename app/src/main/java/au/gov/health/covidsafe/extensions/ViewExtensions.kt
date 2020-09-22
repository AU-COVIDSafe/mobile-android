package au.gov.health.covidsafe.extensions

import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import au.gov.health.covidsafe.BuildConfig
import au.gov.health.covidsafe.R

fun TextView.toHyperlink(textToHyperLink: String? = null, onClick: () -> Unit) {
    val text = this.text
    val spannableString = SpannableString(text)
    val startIndex = if (textToHyperLink.isNullOrEmpty()) {
        0
    } else {
        text.indexOf(textToHyperLink)
    }
    val endIndex = if (textToHyperLink.isNullOrEmpty()) {
        spannableString.length
    } else {
        text.indexOf(textToHyperLink) + textToHyperLink.length
    }
    spannableString.setSpan(URLSpan(""), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    this.setText(spannableString, TextView.BufferType.SPANNABLE)
    this.setLinkTextColor(ContextCompat.getColor(context, R.color.dark_green))
    this.setOnClickListener {
        onClick.invoke()
    }

}

fun Context.shareThisApp() {
    val newIntent = Intent(Intent.ACTION_SEND)
    newIntent.type = "text/plain"
    newIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_this_app_content))
    startActivity(Intent.createChooser(newIntent, null))
}

fun Context.getAppVersionNumberDetails(): String {
    return getString(R.string.home_version_number, BuildConfig.VERSION_NAME)
}

fun fromHtml(stringData: String): Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(stringData, Html.FROM_HTML_MODE_COMPACT)
    } else {
        Html.fromHtml(stringData)
    }
}

fun Context.allPermissionsEnabled(): Boolean {

    val bluetoothEnabled = isBlueToothEnabled() ?: false
    val nonBatteryOptimizationAllowed = isBatteryOptimizationDisabled() ?: true
    val locationStatusAllowed = isLocationPermissionAllowed() ?: true

    return bluetoothEnabled &&
            nonBatteryOptimizationAllowed &&
            locationStatusAllowed &&
            isLocationEnabledOnDevice()
}