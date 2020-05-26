package au.gov.health.covidsafe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecordStorage
import au.gov.health.covidsafe.streetpass.view.RecordViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.database_peek.*
import java.io.File

private const val TAG = "PeekActivity"

class PeekActivity : AppCompatActivity() {

    private lateinit var viewModel: RecordViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        newPeek()
    }

    private fun newPeek() {
        setContentView(R.layout.database_peek)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = RecordListAdapter(this)
        recyclerView.adapter = adapter
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            layoutManager.orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

        viewModel = ViewModelProvider(this).get(RecordViewModel::class.java)
        viewModel.allRecords.observe(this, Observer { records ->
            adapter.setSourceData(records)
        })


        val start = findViewById<FloatingActionButton>(R.id.start)
        start.setOnClickListener {
            startService()
        }

        val stop = findViewById<FloatingActionButton>(R.id.stop)
        stop.setOnClickListener {
            stopService()
        }

        val delete = findViewById<FloatingActionButton>(R.id.delete)
        delete.setOnClickListener { view ->
            view.isEnabled = false

            val builder = AlertDialog.Builder(this)
            builder
                .setTitle("Are you sure?")
                .setCancelable(false)
                .setMessage("Deleting the DB records is irreversible")
                .setPositiveButton("DELETE") { dialog, which ->
                    Observable.create<Boolean> {
                        StreetPassRecordStorage(this).nukeDb()
                        it.onNext(true)
                    }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe { result ->
                            Toast.makeText(this, "Database nuked: $result", Toast.LENGTH_SHORT)
                                .show()
                            view.isEnabled = true
                            dialog.cancel()
                        }
                }

                .setNegativeButton("DON'T DELETE") { dialog, which ->
                    view.isEnabled = true
                    dialog.cancel()
                }

            val dialog: AlertDialog = builder.create()
            dialog.show()

        }


        shareDatabase.setOnClickListener {
            val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
            val databaseFilePath= getDatabasePath("record_database").absolutePath
            val databaseFile = File(databaseFilePath)

            CentralLog.d(TAG, "authority = $authority, databaseFilePath = $databaseFilePath")

            if(databaseFile.exists()) {
                CentralLog.d(TAG, "databaseFile.length = ${databaseFile.length()}")

                FileProvider.getUriForFile(
                        this,
                        authority,
                        databaseFile
                )?.let { databaseFileUri ->
                    CentralLog.d(TAG, "databaseFileUri = $databaseFileUri")

                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "application/octet-stream"
                    intent.putExtra(Intent.EXTRA_STREAM, databaseFileUri)
                    startActivity(Intent.createChooser(intent, "Sharing database"))
                }
            }
        }

        if(!BuildConfig.DEBUG) {
            start.visibility = View.GONE
            stop.visibility = View.GONE
            delete.visibility = View.GONE
        }

    }

    private var timePeriod: Int = 0

    private fun nextTimePeriod(): Int {
        timePeriod = when (timePeriod) {
            1 -> 3
            3 -> 6
            6 -> 12
            12 -> 24
            else -> 1
        }

        return timePeriod
    }


    private fun startService() {
        Utils.startBluetoothMonitoringService(this)
    }

    private fun stopService() {
        Utils.stopBluetoothMonitoringService(this)
    }

}