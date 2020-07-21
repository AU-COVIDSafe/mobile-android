package au.gov.health.covidsafe.talkback

import android.view.View
import android.widget.TextView
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.TracerApp
import au.gov.health.covidsafe.logging.CentralLog
import java.lang.StringBuilder

private const val TAG = "TalkBack.kt"

private fun getHeadingLabel(): String {
    return TracerApp.AppContext.getString(R.string.heading)
}

private fun convertNumberToDigits(originalText: String): String {
    val stringBuilder = StringBuilder()

    originalText.forEach {
        if (it.isDigit()) {
            stringBuilder.append(", ")
        }

        stringBuilder.append(it)
    }

    return stringBuilder.toString()
}

fun setHeading(view: View, shouldConvertNumberToDigits: Boolean = false) {
    if (view is TextView) {
        val content = if (shouldConvertNumberToDigits) {
            convertNumberToDigits(view.text.toString())
        } else {
            view.text.toString()
        }

        view.contentDescription = "${content}, ${getHeadingLabel()}".also {
            CentralLog.d(TAG, it)
        }
    }
}

fun TextView.setHeading(shouldConvertNumberToDigits: Boolean = false) {
    setHeading(this, shouldConvertNumberToDigits)
}