package au.gov.health.covidsafe.ui.base

import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import au.gov.health.covidsafe.extensions.fromHtml
import au.gov.health.covidsafe.ui.home.view.ExternalLinkCard
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("visibility")
fun setVisibility(view: View, isVisible: Boolean?) {
    isVisible?.let {
        view.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}

@BindingAdapter("addUnderline")
fun setUnderlineText(textView: TextView, text: String?) {
    text?.let {
        textView.text = fromHtml("<u>$text</u>")
    }
}

@BindingAdapter("externalCardTitle")
fun setExternalCardTitle(view: ExternalLinkCard, text: Int?) {
    text?.let {
        view.setTitleTextTypeFace(Typeface.DEFAULT_BOLD)
        view.setTitleText(String.format("%,d", it))
    }
}

@BindingAdapter("stateCaseNumberFormat")
fun setStateCaseNumberFormat(view: TextView, text: Int?) {
    text?.let {
        view.text = String.format("%,d", it)
    }
}

@BindingAdapter("dateFormat")
fun setDateFormat(textView: TextView, dateString: String?) {
    dateString?.let {
        val cal = Calendar.getInstance()
        val timeZoneID = "Australia/Sydney"
        val tz = TimeZone.getTimeZone(timeZoneID)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        dateFormat.timeZone = tz
        cal.time = dateFormat.parse(it)
        val convertedDateString = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(cal.time)
        val convertedTimeString = SimpleDateFormat("h a", Locale.getDefault()).format(cal.time)

        val finalDisplayDateFormat = "$convertedDateString at $convertedTimeString AEST"
        textView.text = finalDisplayDateFormat
    }
}
