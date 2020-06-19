package au.gov.health.covidsafe.ui.onboarding

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import au.gov.health.covidsafe.R

class CountryInitialLetterHolder(
    itemView: View,
    private val onLetterClicked: (letter: String) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    fun setLetter(letter: String) {
        val letterTextView = itemView.findViewById<TextView>(R.id.country_initial_letter)
        letterTextView.text = letter
        letterTextView.setOnClickListener {
            onLetterClicked(letter)
        }
    }
}

class CountryInitialLetterRecyclerViewAdapter(
    private val context: Context,
    private val initialLetters: List<String>,
    private val onLetterClicked: (letter: String) -> Unit
) : RecyclerView.Adapter<CountryInitialLetterHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryInitialLetterHolder {
        return CountryInitialLetterHolder(
            LayoutInflater.from(context).inflate(
                R.layout.view_list_item_country_initial_letter,
                parent,
                false
            ),
            onLetterClicked
        )
    }

    override fun getItemCount(): Int {
        return initialLetters.size
    }

    override fun onBindViewHolder(holder: CountryInitialLetterHolder, position: Int) {
        holder.setLetter(initialLetters[position])
    }

}