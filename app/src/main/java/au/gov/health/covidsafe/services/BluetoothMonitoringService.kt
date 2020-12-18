package au.gov.health.covidsafe.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleService
import au.gov.health.covidsafe.BuildConfig
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.app.TracerApp
import au.gov.health.covidsafe.bluetooth.gatt.ReadRequestPayload
import au.gov.health.covidsafe.extensions.isLocationEnabledOnDevice
import au.gov.health.covidsafe.factory.NetworkFactory
import au.gov.health.covidsafe.interactor.usecase.UpdateBroadcastMessageAndPerformScanWithExponentialBackOff
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.notifications.NotificationTemplates
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.sensor.Sensor
import au.gov.health.covidsafe.sensor.SensorArray
import au.gov.health.covidsafe.sensor.SensorDelegate
import au.gov.health.covidsafe.sensor.ble.BLEDevice
import au.gov.health.covidsafe.sensor.ble.BLESensorConfiguration
import au.gov.health.covidsafe.sensor.ble.BLE_TxPower
import au.gov.health.covidsafe.sensor.datatype.*
import au.gov.health.covidsafe.sensor.payload.PayloadDataSupplier
import au.gov.health.covidsafe.streetpass.persistence.Encryption
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecord
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecordDatabase
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecordStorage
import au.gov.health.covidsafe.ui.utils.LocalBlobV2
import au.gov.health.covidsafe.ui.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.CoroutineContext

private const val POWER_SAVE_WHITELIST_CHANGED = "android.os.action.POWER_SAVE_WHITELIST_CHANGED"

@Keep
class BluetoothMonitoringService : LifecycleService(), CoroutineScope, SensorDelegate, PayloadDataSupplier {

    @Keep
    private val bluetoothStatusReceiver = BluetoothStatusReceiver()

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var commandHandler: CommandHandler

    private val awsClient = NetworkFactory.awsClient

    // Sensor for proximity detection
    private var sensor: Sensor? = null
    private lateinit var streetPassRecordStorage: StreetPassRecordStorage
    private var appDelegate: BluetoothMonitoringService = this

    private var recentSaves: MutableMap<String,Date> = HashMap()

    override fun onCreate() {
        super.onCreate()
        AppContext = applicationContext
        setup()
    }

    private fun setup() {
        streetPassRecordStorage = StreetPassRecordStorage(applicationContext)
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

        CentralLog.setPowerManager(pm)
        commandHandler = CommandHandler(WeakReference(this))

        broadcastMessage = Utils.retrieveBroadcastMessage(this.applicationContext)
        unregisterReceivers()
        registerReceivers()

        setupNotifications()
    }

    fun teardown() {
        //commandHandler.removeCallbacksAndMessages(null)

        //Utils.cancelBMUpdateCheck(this.applicationContext)
        //Utils.cancelNextScan(this.applicationContext)
        //Utils.cancelNextAdvertise(this.applicationContext)

        //[AT]
        Log.i(TAG, "[AT] teardown, do nothing")

        //sensor?.stop()
    }

    private fun setupNotifications() {

        val mNotificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = CHANNEL_SERVICE
            // Create the channel for the notification
            val mChannel =
                    NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            mChannel.enableLights(false)
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(0L)
            mChannel.setSound(null, null)
            mChannel.setShowBadge(false)

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun hasLocationPermissions(): Boolean {
        val perms = Utils.getRequiredPermissions()
        return EasyPermissions.hasPermissions(this.applicationContext, *perms)
    }

    private fun isLocationPermissionEnabled(): Boolean {
        return hasLocationPermissions() && this.isLocationEnabledOnDevice()
    }

    private fun isBluetoothEnabled(): Boolean {
        var btOn = false
        val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }

        bluetoothAdapter?.let {
            btOn = it.isEnabled
        }
        return btOn
    }

    private fun isBatteryOptimizationDisabled(): Boolean {
        return try {
            val powerManager = TracerApp.AppContext.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager?
            val packageName = TracerApp.AppContext.packageName

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager?.isIgnoringBatteryOptimizations(packageName) ?: true
            } else {
                true
            }
        } catch (e: Exception) {
            CentralLog.e(TAG, "isBatteryOptimizationDisabled() throws exception", e)
            true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        CentralLog.i(TAG, "Service onStartCommand")

        //check for permissions
        if (!isLocationPermissionEnabled() || !isBluetoothEnabled() || !isBatteryOptimizationDisabled()) {
            CentralLog.i(
                    TAG,
                    "location permission: ${isLocationPermissionEnabled()} bluetooth: ${isBluetoothEnabled()}"
            )
            showForegroundNotification()
            return START_STICKY
        }

        intent?.let {
            val cmd = intent.getIntExtra(COMMAND_KEY, Command.INVALID.index)
            runService(Command.findByValue(cmd))

            return START_STICKY
        }

        if (intent == null) {
            CentralLog.e(TAG, "Nothing in intent @ onStartCommand")
            commandHandler.startBluetoothMonitoringService()
        }

        // Tells the system to not try to recreate the service after it has been killed.
        return START_STICKY
    }

    fun runService(cmd: Command?) {

        CentralLog.i(TAG, "Command is:${cmd?.string}")

        //check for permissions
        if (!isLocationPermissionEnabled() || !isBluetoothEnabled()) {
            CentralLog.i(
                    TAG,
                    "location permission: ${isLocationPermissionEnabled()} bluetooth: ${isBluetoothEnabled()}"
            )
            showForegroundNotification()
            return
        }

        when (cmd) {
            Command.ACTION_START -> {
                actionStart()
                Utils.scheduleNextHealthCheck(this.applicationContext, healthCheckInterval)
                Utils.scheduleBMUpdateCheck(this.applicationContext, bmCheckInterval)
            }

            Command.ACTION_SCAN -> {
            }

            Command.ACTION_ADVERTISE -> {
            }

            Command.ACTION_UPDATE_BM -> {
                actionUpdateBm()
            }

            Command.ACTION_STOP -> {
                actionStop()
            }

            Command.ACTION_SELF_CHECK -> {
                actionHealthCheck()
            }

            else -> CentralLog.i(TAG, "Invalid command: $cmd. Nothing to do")
        }
    }

    private fun actionStart() {
        if (Preference.isOnBoarded(this)) {
            CentralLog.d(TAG, "Service Starting ")

            startForeground(
                    NOTIFICATION_ID,
                    NotificationTemplates.getRunningNotification(
                            this.applicationContext,
                            CHANNEL_ID
                    )
            )
            //ensure BM is ready here
            if (Preference.isOnBoarded(this) && Utils.needToUpdate(this.applicationContext) || broadcastMessage == null) {
                //need to pull new BM

                UpdateBroadcastMessageAndPerformScanWithExponentialBackOff(awsClient, applicationContext, lifecycle).invoke(
                        params = null,
                        onSuccess = {
                            broadcastMessage = it.tempId
                            sensorStart()
                        },
                        onFailure = {
                        }
                )
            }
            sensorStart()
        }
    }

    fun sensorStart() {
        // [AT] Don't create a new sensor, to avoid creating multiple advertisements/duplicate GATT services
        Log.i(TAG, "[AT] sensorStart")

        if (broadcastMessage != null && sensor == null) {

            streetPassRecordStorage = StreetPassRecordStorage(applicationContext)

            Log.i(TAG, "[AT] creating a new SensorArray")

            sensor = SensorArray(applicationContext, this)
            getAppDelegate().sensor()?.add(this)
            // Sensor will start and stop with Bluetooth power on / off events
            sensor?.start()
        }
    }

    /// Get app delegate
    fun getAppDelegate(): BluetoothMonitoringService {
        return appDelegate
    }

    /// Get sensor
    fun sensor(): Sensor? {
        return sensor
    }

    private fun actionStop() {
        stopForeground(true)
        stopSelf()
        CentralLog.w(TAG, "Service Stopping")
    }

    private fun actionHealthCheck() {
        Utils.scheduleNextHealthCheck(this.applicationContext, healthCheckInterval)
        performHealthCheck()
    }

    private fun actionUpdateBm() {
        Utils.scheduleBMUpdateCheck(this.applicationContext, bmCheckInterval)

        CentralLog.i(TAG, "checking need to update BM")
        if (Preference.isOnBoarded(this) && Utils.needToUpdate(this.applicationContext) || broadcastMessage == null) {
            //need to pull new BM

            UpdateBroadcastMessageAndPerformScanWithExponentialBackOff(awsClient, applicationContext, lifecycle).invoke(
                    params = null,
                    onSuccess = {
                        broadcastMessage = it.tempId
                    },
                    onFailure = {
                    }
            )
        } else {
            CentralLog.i(TAG, "Don't need to update bm")
        }
    }

    private fun performHealthCheck() {

        CentralLog.i(TAG, "Performing self diagnosis")

        if (!isLocationPermissionEnabled() || !isBluetoothEnabled() || !isBatteryOptimizationDisabled()) {
            CentralLog.i(TAG, "no location permission")
            showForegroundNotification()
            return
        }

        startForeground(
                NOTIFICATION_ID,
                NotificationTemplates.getRunningNotification(
                        this.applicationContext,
                        CHANNEL_ID
                )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        CentralLog.i(TAG, "BluetoothMonitoringService destroyed - tearing down")

        teardown()
        unregisterReceivers()

        job.cancel()

        CentralLog.i(TAG, "BluetoothMonitoringService destroyed")
    }

    private fun showForegroundNotification() {

        launch(Dispatchers.Main) {

            val notificationContentText: Int = if (!isLocationPermissionEnabled() && isBluetoothEnabled() && isBatteryOptimizationDisabled()) {
                //Location Disabled
                R.string.notification_location
            } else if (!isBluetoothEnabled() && isLocationPermissionEnabled() && isBatteryOptimizationDisabled()) {
                //Bluetooth Disabled
                R.string.notification_bluetooth
            } else if (!isBatteryOptimizationDisabled() && isLocationPermissionEnabled() && isBluetoothEnabled()) {
                //Battery optimization Disabled
                R.string.notification_battery
            } else if (!isBatteryOptimizationDisabled() || !isLocationPermissionEnabled() || !isBluetoothEnabled()) {
                //Multiple permission Disabled
                R.string.notification_settings
            } else {
                //All permission are enabled, so we should show Active message.
                -1
            }

            val notificationMessage = if (notificationContentText > 0) {
                NotificationTemplates.lackingThingsNotification(
                        this@BluetoothMonitoringService.applicationContext,
                        notificationContentText,
                        CHANNEL_ID)
            } else {
                //All permissions are enabled
                NotificationTemplates.getRunningNotification(this@BluetoothMonitoringService.applicationContext, CHANNEL_ID)
            }

            startForeground(NOTIFICATION_ID, notificationMessage)
        }
    }

    private val gpsSwitchStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.action?.let {
                if (it == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    CentralLog.i(TAG, "Location ON/OFF status changed")
                    showForegroundNotification()
                }
            }
        }
    }

    private val powerStateChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.action?.let {
                if (it == POWER_SAVE_WHITELIST_CHANGED) {
                    CentralLog.i(TAG, "Save mode status changed")
                    showForegroundNotification()
                }
            }
        }
    }

    private fun unregisterReceivers() {
        try {
            unregisterReceiver(bluetoothStatusReceiver)

        } catch (e: Throwable) {
            CentralLog.w(TAG, "bluetoothStatusReceiver is not registered?")
        }

        unregisterLocationReceiver()
        unregisterPowerStateChangeReceiver()
    }

    private fun unregisterLocationReceiver() {
        try {
            unregisterReceiver(gpsSwitchStateReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "Location Receiver is not registered?")
        }
    }

    private fun unregisterPowerStateChangeReceiver() {
        try {
            unregisterReceiver(powerStateChangeReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "Power State Receiver is not registered?")
        }
    }

    private fun registerLocationChangeReceiver() {
        registerReceiver(gpsSwitchStateReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    }

    private fun registerPowerModeChangeReceiver() {
        registerReceiver(powerStateChangeReceiver, IntentFilter(POWER_SAVE_WHITELIST_CHANGED))
    }

    private fun registerReceivers() {
        val bluetoothStatusReceivedFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStatusReceiver, bluetoothStatusReceivedFilter)

        registerLocationChangeReceiver()
        registerPowerModeChangeReceiver()

        CentralLog.i(TAG, "Receivers registered")
    }

    inner class BluetoothStatusReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val action = intent.action
                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {

                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_TURNING_OFF")
                            showForegroundNotification()
                            teardown()
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_OFF")
                        }
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_TURNING_ON")
                        }
                        BluetoothAdapter.STATE_ON -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_ON")
                            Utils.startBluetoothMonitoringService(this@BluetoothMonitoringService.applicationContext)
                            showForegroundNotification()
                        }
                    }
                }
            }
        }
    }

    enum class Command(val index: Int, val string: String) {
        INVALID(-1, "INVALID"),
        ACTION_START(0, "START"),
        ACTION_SCAN(1, "SCAN"),
        ACTION_STOP(2, "STOP"),
        ACTION_ADVERTISE(3, "ADVERTISE"),
        ACTION_SELF_CHECK(4, "SELF_CHECK"),
        ACTION_UPDATE_BM(5, "UPDATE_BM");

        companion object {
            private val types = values().associate { it.index to it }
            fun findByValue(value: Int) = types[value]
        }
    }

    override fun sensor(sensor: SensorType?, didDetect: TargetIdentifier?) {
        CentralLog.d(TAG, "${sensor?.name} ,didDetect= $didDetect")
    }

    override fun sensor(sensor: SensorType?, didRead: PayloadData?, fromTarget: TargetIdentifier?) {
        CentralLog.d(TAG, "${sensor?.name} ,didRead= ${didRead?.shortName()} ,fromTarget= $fromTarget")
    }

    override fun sensor(sensor: SensorType?, didShare: MutableList<PayloadData>?, fromTarget: TargetIdentifier?) {
        val payloads: MutableList<String> = ArrayList(didShare!!.size)
        for (payloadData in didShare) {
            payloads.add(payloadData.shortName())
        }
        CentralLog.d(TAG, "${sensor?.name} ,didShare= $payloads ,fromTarget= $fromTarget")
    }

    override fun sensor(sensor: SensorType?, didMeasure: Proximity?, fromTarget: TargetIdentifier?) {
        CentralLog.d(TAG, "${sensor?.name} ,didMeasure= ${didMeasure?.description()} ,fromTarget= $fromTarget")
    }

    override fun sensor(sensor: SensorType?, didVisit: Location?) {
        CentralLog.d(TAG, "${sensor?.name} ,didVisit= ${didVisit?.description()}")
    }

    override fun sensor(sensor: SensorType?, didMeasure: Proximity?, fromTarget: TargetIdentifier?, withPayload: PayloadData?, device: BLEDevice) {
        CentralLog.d(TAG, "${sensor?.name} ,didMeasure= ${didMeasure?.description()} ,fromTarget= ${fromTarget} ,withPayload= ${withPayload?.shortName()}, withDevice=$device")
        wrtieEncounterRecordToDB(device)
    }

    override fun sensor(sensor: SensorType?, didMeasure: Proximity?, fromTarget: TargetIdentifier?, withPayload: PayloadData?){
        CentralLog.d(TAG, "${sensor?.name} ,didMeasure= ${didMeasure?.description()} ,fromTarget= ${fromTarget} ,withPayload= ${withPayload?.shortName()}")
    }

    override fun sensor(sensor: SensorType?, didUpdateState: SensorState?) {
        CentralLog.d(TAG, "${sensor?.name} ,didUpdateState= ${didUpdateState?.name}")
    }

    override fun sensor(sensor: SensorType?, didRead: PayloadData?, fromTarget: TargetIdentifier?, atProximity: Proximity?, withTxPower: Int, device: BLEDevice) {
        CentralLog.d(TAG, "${sensor?.name} ,fromTarget= $fromTarget , atProximity= ${atProximity}, withTxPower= $withTxPower")
        //wrtieEncounterRecordToDB(device)
    }

    private fun cleanRecentSaves() {
        recentSaves = recentSaves.filter { (key, value) -> TimeInterval( Date().time - value.time).value < BuildConfig.PERIPHERAL_PAYLOAD_SAVE_INTERVAL} as MutableMap<String, Date>
    }

    private fun wrtieEncounterRecordToDB(device: BLEDevice): Any {

        return try {

            var deviceId: String = if(device.pseudoDeviceAddress()==null)device.identifier.value else device.pseudoDeviceAddress().toString()
            cleanRecentSaves()
            if(device.payloadData() != null && !recentSaves.containsKey(deviceId)) {
                recentSaves.put(deviceId,Date())

                val didRead = device.payloadData()
                val deviceRssi = device.rssi()
                val withTxPower = device.txPower()

                val peripheralrecord = ReadRequestPayload.gson.fromJson(String(didRead.value), ReadRequestPayload::class.java)
                val modelC = if (StreetPassRecordDatabase.DUMMY_DEVICE == TracerApp.asCentralDevice().modelC) null else TracerApp.asCentralDevice().modelC
                val modelP = if (StreetPassRecordDatabase.DUMMY_DEVICE == peripheralrecord.modelP) null else peripheralrecord.modelP
                val rssi = if (deviceRssi?.value == StreetPassRecordDatabase.DUMMY_RSSI) null else deviceRssi.value
                val txPower = if (withTxPower == null || withTxPower.value == StreetPassRecordDatabase.DUMMY_TXPOWER) null else withTxPower.value
                val plainLocalBlob = ReadRequestPayload.gson.toJson(LocalBlobV2(
                        modelP,
                        modelC,
                        txPower,
                        rssi
                )).toByteArray(Charsets.UTF_8)

                val localBlob = Encryption.encryptPayload(plainLocalBlob)
                val record = StreetPassRecord(
                        v = peripheralrecord.v,
                        org = peripheralrecord.org,
                        localBlob = localBlob,
                        remoteBlob = peripheralrecord.msg
                )

                launch {
                    streetPassRecordStorage.saveRecord(record)
                }
            }else{}
        } catch (e: java.lang.Exception) {
            CentralLog.d(TAG, "Json parsing failed = $e")
        }
    }

    override fun payload(data: Data?): MutableList<PayloadData> {
        // Split raw data comprising of concatenated payloads into individual payloads, in our case we will only every get one payload at a time
        val payload = PayloadData(data?.value)
        val payloads: MutableList<PayloadData> = ArrayList()
        payloads.add(payload)
        return payloads
    }

    inner class ReadRequestEncryptedPayload(val timestamp: Long, val modelP: String, val msg: String?)

    override fun payload(timestamp: PayloadTimestamp?): PayloadData {
        val peripheral = TracerApp.asPeripheralDevice()
        val readRequest = ReadRequestEncryptedPayload(
                System.currentTimeMillis() / 1000L,
                peripheral.modelP,
                thisDeviceMsg()
        )
        val plainRecord = ReadRequestPayload.gson.toJson(readRequest)

        CentralLog.d(TAG, "onCharacteristicReadRequest plainRecord =  $plainRecord")

        val plainRecordByteArray = plainRecord.toByteArray(Charsets.UTF_8)
        val remoteBlob = Encryption.encryptPayload(plainRecordByteArray)
        val base =
                ReadRequestPayload(
                        v = TracerApp.protocolVersion,
                        msg = remoteBlob,
                        org = TracerApp.ORG,
                        modelP = null //This is going to be stored as empty in the db as DUMMY value
                ).getPayload()
        val value = base.copyOfRange(0, base.size)
        return PayloadData(value)
    }

    companion object {

        private const val TAG = "BTMService"

        private const val NOTIFICATION_ID = BuildConfig.SERVICE_FOREGROUND_NOTIFICATION_ID
        private const val CHANNEL_ID = BuildConfig.SERVICE_FOREGROUND_CHANNEL_ID
        const val CHANNEL_SERVICE = BuildConfig.SERVICE_FOREGROUND_CHANNEL_NAME

        const val COMMAND_KEY = "${BuildConfig.APPLICATION_ID}_CMD"

        const val PENDING_ACTIVITY = 5
        const val PENDING_START = 6
        const val PENDING_SCAN_REQ_CODE = 7
        const val PENDING_ADVERTISE_REQ_CODE = 8
        const val PENDING_HEALTH_CHECK_CODE = 9
        const val PENDING_WIZARD_REQ_CODE = 10
        const val PENDING_BM_UPDATE = 11
        const val PENDING_PRIVACY_CLEANER_CODE = 12

        var broadcastMessage: String? = null

        const val maxQueueTime: Long = BuildConfig.MAX_QUEUE_TIME
        const val bmCheckInterval: Long = BuildConfig.BM_CHECK_INTERVAL
        const val healthCheckInterval: Long = BuildConfig.HEALTH_CHECK_INTERVAL
        const val connectionTimeout: Long = BuildConfig.CONNECTION_TIMEOUT

        const val blacklistDuration: Long = BuildConfig.BLACKLIST_DURATION
        lateinit var AppContext: Context
        fun thisDeviceMsg(): String? {
            broadcastMessage?.let {
                CentralLog.i(TAG, "Retrieved BM for storage: $it")
                return it
            }
            CentralLog.e(TAG, "No local Broadcast Message")
            return ""
        }
    }
}
