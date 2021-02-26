package au.gov.health.covidsafe.ui.restriction

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.networking.response.Subheadings

class RestrictionAdapter internal constructor(context: Context) :
        RecyclerView.Adapter<RestrictionAdapter.RestrictionViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var restrictionList = emptyList<Subheadings>()
    private var mListener: OnStateListClickListener? = null

    interface OnStateListClickListener {
        fun onSectionClick(title: String?, content: String?)
    }

    inner class RestrictionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtRestriction: TextView = itemView.findViewById(R.id.restriction_title)
        val listLayout: ConstraintLayout = itemView.findViewById(R.id.restriction_layout)
    }

    fun setOnStateListClickListener(actionListener: OnStateListClickListener) {
        mListener = actionListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestrictionAdapter.RestrictionViewHolder {
        val itemView = inflater.inflate(R.layout.view_list_item_restriction, parent, false)
        return RestrictionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RestrictionAdapter.RestrictionViewHolder, position: Int) {
        holder.txtRestriction.text = restrictionList[position].title

        holder.listLayout.setOnClickListener {
            // setRecords(reestrictionList)
            mListener?.onSectionClick(restrictionList[position].title, restrictionList[position].content)
        }
    }

    fun setRecords(list: List<Subheadings>) {
        this.restrictionList = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = restrictionList.size

}
