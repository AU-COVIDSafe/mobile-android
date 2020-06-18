package au.gov.health.covidsafe.ui.home.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.TracerApp
import kotlinx.android.synthetic.main.view_card_permission_card.view.*

class PermissionStatusCard @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_card_permission_card, this, true)

        val a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.PermissionStatusCard)
        val title = a.getString(R.styleable.PermissionStatusCard_permissionStatusCard_title)
        a.recycle()

        permission_title.text = title

        val height = context.resources.getDimensionPixelSize(R.dimen.permission_height)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
    }

    fun render(title: String, correct: Boolean, body: String? = null) {
        val errorTextColor = ContextCompat.getColor(TracerApp.AppContext, R.color.error)
        val normalTextColor = ContextCompat.getColor(TracerApp.AppContext, R.color.slack_black)

        permission_icon.isSelected = correct
        permission_title.text = title
        permission_title.setTextColor(if (correct) normalTextColor else errorTextColor)

        if (correct || body == null) {
            permission_body.visibility = View.GONE
        } else {
            permission_body.visibility = View.VISIBLE
            permission_body.text = body
            permission_body.setTextColor(if (correct) normalTextColor else errorTextColor)
        }
    }
}