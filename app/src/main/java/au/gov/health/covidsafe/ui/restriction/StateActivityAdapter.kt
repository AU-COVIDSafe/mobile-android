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
import au.gov.health.covidsafe.networking.response.Activities
import au.gov.health.covidsafe.networking.response.Subheadings


class StateActivityAdapter internal constructor(context: Context) :
        RecyclerView.Adapter<StateActivityAdapter.StateViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var stateList = emptyList<Activities>()
    private var selectedState: Int = 0
    private var mListener: OnStateListClickListener? = null

    interface OnStateListClickListener {
        fun onStateClick(subHeading: List<Subheadings>, activity: String?, activityTitle: String?, time: String?, content: String?)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StateActivityAdapter.StateViewHolder {
        val itemView = inflater.inflate(R.layout.view_list_item_state, parent, false)
        return StateViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StateActivityAdapter.StateViewHolder, position: Int) {
        holder.txtStateName.text = stateList[position].activitiyTitle
        holder.imageStateSelect.visibility = View.GONE
        holder.endLine.visibility = View.VISIBLE

        if (position == selectedState) {
            holder.imageStateSelect.visibility = View.VISIBLE
        }
        if (position == (this.stateList.size - 1)) {
            holder.endLine.visibility = View.GONE
        }
        holder.countryListLayout.setOnClickListener {
            setRecords(stateList, position)
            mListener?.onStateClick(stateList[position].subheadings, stateList[position].activity, stateList[position].activitiyTitle, stateList[position].contentDateTitle, stateList[position].content)
        }
    }

    fun setRecords(stateList: List<Activities>, selectedState: Int) {
        this.stateList = stateList
        this.selectedState = selectedState
        notifyDataSetChanged()
    }

    override fun getItemCount() = stateList.size

}
