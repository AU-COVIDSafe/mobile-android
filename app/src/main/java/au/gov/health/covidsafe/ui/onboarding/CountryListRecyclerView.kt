package au.gov.health.covidsafe.ui.onboarding

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.app.TracerApp

const val VIEW_TYPE_GROUP_TITLE = 1
const val VIEW_TYPE_COUNTRY = 2
const val NOLFOLK_ISLAND = 672
const val AUS = 61

interface CountryListItemInterface

class CountryListItem(
        val countryNameResId: Int,
        val callingCode: Int,
        val flagResID: Int
) : CountryListItemInterface

class CountryGroupTitle(
        private val titleResId: Int?,
        private val title: String? = null
) : CountryListItemInterface {
    fun getTitle(context: Context): String {
        return when (title) {
            null -> titleResId?.let {
                context.getString(it)
            } ?: ""
            else -> title
        }
    }
}


class CountryGroupTitleHolder(
        itemView: View
) : RecyclerView.ViewHolder(itemView) {
    fun setCountryGroupTitle(title: String) {
        itemView.findViewById<TextView>(R.id.country_group_title).text = title
    }
}

class CountryListItemHolder(
        itemView: View,
        private val onCountryClicked: () -> Unit
) : RecyclerView.ViewHolder(itemView) {
    private var countryNameResId: Int = 0
    private var callingCode: Int = 0
    private var flagResID: Int = 0

    fun setCountryListItem(countryNameResId: Int,
                           countryName: String,
                           callingCode: Int,
                           flagResID: Int) {
        this.countryNameResId = countryNameResId
        this.callingCode = callingCode
        this.flagResID = flagResID

        itemView.findViewById<TextView>(R.id.country_list_name).text = countryName
        itemView.findViewById<TextView>(R.id.country_list_calling_code).text = "+$callingCode"
        itemView.findViewById<ImageView>(R.id.country_list_flag).setImageResource(flagResID)

        itemView.findViewById<View>(R.id.country_list_item).setOnClickListener {
            Preference.putCountryNameResID(TracerApp.AppContext, countryNameResId)
            Preference.putCallingCode(TracerApp.AppContext, callingCode)
            Preference.putNationalFlagResID(TracerApp.AppContext, flagResID)
            onCountryClicked()
        }
    }
}

class CountryListRecyclerViewAdapter(
        private val context: Context,
        private val countryListItem: List<CountryListItemInterface>,
        private val onCountryClicked: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_GROUP_TITLE -> CountryGroupTitleHolder(
                    LayoutInflater.from(context).inflate(
                            R.layout.view_list_item_group_title,
                            parent,
                            false
                    )
            )
            else -> CountryListItemHolder(
                    LayoutInflater.from(context).inflate(
                            R.layout.view_list_item_country,
                            parent,
                            false
                    ),
                    onCountryClicked
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (countryListItem[position]) {
            is CountryGroupTitle -> VIEW_TYPE_GROUP_TITLE
            else -> VIEW_TYPE_COUNTRY
        }
    }

    override fun getItemCount(): Int {
        return countryListItem.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CountryGroupTitleHolder -> {
                val countryGroupTitle = countryListItem[position] as CountryGroupTitle
                holder.setCountryGroupTitle(countryGroupTitle.getTitle(context))
            }

            is CountryListItemHolder -> {
                val countryListItem = (countryListItem[position] as CountryListItem)
                val countryName = context.getString(countryListItem.countryNameResId)

                holder.setCountryListItem(
                        countryListItem.countryNameResId,
                        countryName,
                        countryListItem.callingCode,
                        countryListItem.flagResID
                )
            }
        }
    }
}