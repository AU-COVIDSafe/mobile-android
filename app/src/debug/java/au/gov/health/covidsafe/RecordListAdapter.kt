package au.gov.health.covidsafe

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecord
import au.gov.health.covidsafe.streetpass.view.StreetPassRecordViewModel
import au.gov.health.covidsafe.ui.utils.Utils


class RecordListAdapter internal constructor(context: Context) :
        RecyclerView.Adapter<RecordListAdapter.RecordViewHolder>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var records = emptyList<StreetPassRecordViewModel>() // Cached copy of records
    private var sourceData = emptyList<StreetPassRecord>()

    inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val modelCView: TextView = itemView.findViewById(R.id.modelc)
        val modelPView: TextView = itemView.findViewById(R.id.modelp)
        val timestampView: TextView = itemView.findViewById(R.id.timestamp)
        val findsView: TextView = itemView.findViewById(R.id.finds)
        val txpowerView: TextView = itemView.findViewById(R.id.txpower)
        val signalStrengthView: TextView = itemView.findViewById(R.id.signal_strength)
        val filterModelP: View = itemView.findViewById(R.id.filter_by_modelp)
        val filterModelC: View = itemView.findViewById(R.id.filter_by_modelc)
        val msgView: TextView = itemView.findViewById(R.id.msg)
        val version: TextView = itemView.findViewById(R.id.version)
        val org: TextView = itemView.findViewById(R.id.org)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val itemView = inflater.inflate(R.layout.recycler_view_item, parent, false)
        return RecordViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val current = records[position]
        holder.msgView.text = current.msg
        holder.modelCView.text = current.modelC
        holder.modelPView.text = current.modelP
        holder.findsView.text = "Detections: ${current.number}"
        val readableDate = Utils.getDate(current.timeStamp)
        holder.timestampView.text = readableDate
        holder.version.text = "v: ${current.version}"
        holder.org.text = "ORG: ${current.org}"

        holder.filterModelP.tag = current
        holder.filterModelC.tag = current

        holder.signalStrengthView.text = "Signal Strength: ${current.rssi}"

        holder.txpowerView.text = "Tx Power: ${current.transmissionPower}"

    }

    private fun setRecords(records: List<StreetPassRecordViewModel>) {
        this.records = records
        notifyDataSetChanged()
    }

    internal fun setSourceData(records: List<StreetPassRecord>) {
        this.sourceData = records
        setRecords(prepareViewData(this.sourceData))
    }

    private fun prepareViewData(words: List<StreetPassRecord>): List<StreetPassRecordViewModel> {

        words.let {

            val reversed = it.reversed()
            return reversed.map { streetPassRecord ->
                return@map StreetPassRecordViewModel(streetPassRecord)
            }
        }
    }

    override fun getItemCount() = records.size

}