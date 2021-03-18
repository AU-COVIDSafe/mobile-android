package au.gov.health.covidsafe.ui.restriction

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import au.gov.health.covidsafe.R

class StateAdapter internal constructor(context: Context) :
        RecyclerView.Adapter<StateAdapter.StateViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var stateList = emptyList<String>()
    private var selectedState: Int? = null
    private var mListener: OnStateListClickListener? = null

    interface OnStateListClickListener {
        fun onStateClick(state: String)
    }

    inner class StateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtStateName: TextView = itemView.findViewById(R.id.state_name)
        val imageStateSelect: ImageView = itemView.findViewById(R.id.img_state_select)
        val endLine: View = itemView.findViewById(R.id.end_line)
        val countryListLayout: ConstraintLayout = itemView.findViewById(R.id.country_list_item)
    }

    fun setOnStateListClickListener(actionListener: OnStateListClickListener) {
        mListener = actionListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StateAdapter.StateViewHolder {
        val itemView = inflater.inflate(R.layout.view_list_item_state, parent, false)
        return StateViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StateAdapter.StateViewHolder, position: Int) {
        holder.txtStateName.text = stateList[position]
        holder.imageStateSelect.visibility = View.GONE
        holder.endLine.visibility = View.VISIBLE

        if (selectedState != null && position == selectedState) {
            holder.imageStateSelect.visibility = View.VISIBLE
        }
        if (position == (this.stateList.size - 1)) {
            holder.endLine.visibility = View.GONE
        }
        holder.countryListLayout.setOnClickListener {
            setRecords(stateList, position)
            mListener?.onStateClick(stateList[position])
        }
    }

    fun setRecords(stateList: List<String>, selectedState: Int?) {
        this.stateList = stateList
        selectedState?.let { this.selectedState = it }
        notifyDataSetChanged()
    }

    override fun getItemCount() = stateList.size
}
