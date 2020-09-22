package au.gov.health.covidsafe.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Base64
import android.util.Base64.decode
import au.gov.health.covidsafe.BuildConfig
import au.gov.health.covidsafe.ui.utils.Utils
import au.gov.health.covidsafe.bluetooth.BLEScanner.FilterConstant.APPLE_MANUFACTURER_ID
import au.gov.health.covidsafe.bluetooth.BLEScanner.FilterConstant.BACKGROUND_IOS_SERVICE_UUID
import au.gov.health.covidsafe.logging.CentralLog
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

class BLEScanner constructor(context: Context, uuid: String, reportDelay: Long) {

    object FilterConstant {
        const val APPLE_MANUFACTURER_ID = 76
        val BACKGROUND_IOS_SERVICE_UUID: ByteArray = decode(BuildConfig.IOS_BACKGROUND_UUID, Base64.DEFAULT)
    }

    private var serviceUUID: String by Delegates.notNull()
    private var context: Context by Delegates.notNull()
    private var scanCallback: ScanCallback? = null
    private var reportDelay: Long by Delegates.notNull()

    private var scanner: BluetoothLeScanner? =
            BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner

    private val TAG = "BLEScanner"

    init {
        this.serviceUUID = uuid
        this.context = context
        this.reportDelay = reportDelay
    }

    fun startScan(scanCallback: ScanCallback) {
        val serviceUUIDFilter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UUID.fromString(serviceUUID)))
                .build()

        val backgroundedIPhoneFilter = ScanFilter.Builder()
                .setServiceUuid(null)
                .setManufacturerData(
                        APPLE_MANUFACTURER_ID,
                        BACKGROUND_IOS_SERVICE_UUID
                )
                .build()

        val filters: ArrayList<ScanFilter> = ArrayList()
        filters.add(serviceUUIDFilter)
        filters.add(backgroundedIPhoneFilter)

        val settings = ScanSettings.Builder()
                .setReportDelay(reportDelay)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

        this.scanCallback = scanCallback
        scanner = scanner ?: BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
        scanner?.startScan(filters, settings, scanCallback)
    }

    fun stopScan() {

        try {
            if (scanCallback != null && Utils.isBluetoothAvailable()) { //fixed crash if BT if turned off, stop scan will crash.
                scanner?.stopScan(scanCallback)
                CentralLog.d(TAG, "scanning stopped")
            }
        } catch (e: Throwable) {
            CentralLog.e(
                    TAG,
                    "unable to stop scanning - callback null or bluetooth off? : ${e.localizedMessage}"
            )
        }
    }

}
