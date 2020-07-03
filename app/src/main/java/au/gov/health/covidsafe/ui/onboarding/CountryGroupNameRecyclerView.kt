package au.gov.health.covidsafe.ui.onboarding

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import au.gov.health.covidsafe.R

class CountryGroupNameHolder(
    itemView: View,
    private val onGroupNameClicked: (groupName: String) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    fun setLetter(letter: String) {
        val letterTextView = itemView.findViewById<TextView>(R.id.country_group_name)
        letterTextView.text = letter
        letterTextView.setOnClickListener {
            onGroupNameClicked(letter)
        }
    }
}

class CountryGroupNameRecyclerViewAdapter(
        private val context: Context,
        private val groupNames: List<String>,
        private val onGroupNameClicked: (groupName: String) -> Unit
) : RecyclerView.Adapter<CountryGroupNameHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryGroupNameHolder {
        return CountryGroupNameHolder(
            LayoutInflater.from(context).inflate(
                R.layout.view_list_item_country_group_name,
                parent,
                false
            ),
            onGroupNameClicked
        )
    }

    override fun getItemCount(): Int {
        return groupNames.size
    }

    override fun onBindViewHolder(holder: CountryGroupNameHolder, position: Int) {
        holder.setLetter(groupNames[position])
    }

}