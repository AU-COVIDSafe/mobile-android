package au.gov.health.covidsafe.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import au.gov.health.covidsafe.*
import au.gov.health.covidsafe.bluetooth.BLEAdvertiser
import au.gov.health.covidsafe.bluetooth.gatt.ACTION_RECEIVED_STATUS
import au.gov.health.covidsafe.bluetooth.gatt.ACTION_RECEIVED_STREETPASS
import au.gov.health.covidsafe.bluetooth.gatt.STATUS
import au.gov.health.covidsafe.bluetooth.gatt.STREET_PASS
import au.gov.health.covidsafe.factory.NetworkFactory
import au.gov.health.covidsafe.interactor.usecase.UpdateBroadcastMessageAndPerformScanWithExponentialBackOff
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.notifications.NotificationTemplates
import au.gov.health.covidsafe.receivers.PrivacyCleanerReceiver
import au.gov.health.covidsafe.status.Status
import au.gov.health.covidsafe.status.persistence.StatusRecord
import au.gov.health.covidsafe.status.persistence.StatusRecordStorage
import au.gov.health.covidsafe.streetpass.ConnectionRecord
import au.gov.health.covidsafe.streetpass.StreetPassScanner
import au.gov.health.covidsafe.streetpass.StreetPassServer
import au.gov.health.covidsafe.streetpass.StreetPassWorker
import au.gov.health.covidsafe.streetpass.persistence.Encryption
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecord
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecordDatabase
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecordDatabase.Companion.DUMMY_DEVICE
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecordDatabase.Companion.DUMMY_RSSI
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecordDatabase.Companion.DUMMY_TXPOWER
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecordDatabase.Companion.ENCRYPTED_EMPTY_DICT
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecordDatabase.Companion.VERSION_ONE
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecordStorage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions
import java.lang.Exception
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

@Keep
class BluetoothMonitoringService : LifecycleService(), CoroutineScope {

    private var mNotificationManager: NotificationManager? = null

    @Keep
    private lateinit var serviceUUID: String

    private var streetPassServer: StreetPassServer? = null
    private var streetPassScanner: StreetPassScanner? = null
    private var advertiser: BLEAdvertiser? = null

    private var worker: StreetPassWorker? = null

    private val streetPassReceiver = StreetPassReceiver()
    private val statusReceiver = StatusReceiver()
    private val bluetoothStatusReceiver = BluetoothStatusReceiver()

    private lateinit var streetPassRecordStorage: StreetPassRecordStorage
    private lateinit var statusRecordStorage: StatusRecordStorage

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var commandHandler: CommandHandler

    private lateinit var mService: SensorMonitoringService
    private var mBound: Boolean = false

    private lateinit var localBroadcastManager: LocalBroadcastManager

    private val awsClient = NetworkFactory.awsClient

    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()


    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as SensorMonitoringService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        setup()
    }

    private fun setup() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

        CentralLog.setPowerManager(pm)

        commandHandler = CommandHandler(WeakReference(this))

        CentralLog.d(TAG, "Creating service - BluetoothMonitoringService")
        serviceUUID = BuildConfig.BLE_SSID

        worker = StreetPassWorker(this.applicationContext)

        unregisterReceivers()
        registerReceivers()

        streetPassRecordStorage = StreetPassRecordStorage(this.applicationContext)
        statusRecordStorage = StatusRecordStorage(this.applicationContext)
        PrivacyCleanerReceiver.startAlarm(this.applicationContext)
        setupNotifications()
        broadcastMessage = Utils.retrieveBroadcastMessage(this.applicationContext)
    }

    fun teardown() {
        streetPassServer?.tearDown()
        streetPassServer = null

        streetPassScanner?.stopScan()
        streetPassScanner = null

        commandHandler.removeCallbacksAndMessages(null)

        Utils.cancelBMUpdateCheck(this.applicationContext)
        Utils.cancelNextScan(this.applicationContext)
        Utils.cancelNextAdvertise(this.applicationContext)
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

        // Bind to LocalService
        Intent(this.applicationContext, SensorMonitoringService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        //check for permissions
        if (!hasLocationPermissions() || !isBluetoothEnabled() || !isBatteryOptimizationDisabled()) {
            CentralLog.i(
                    TAG,
                    "location permission: ${hasLocationPermissions()} bluetooth: ${isBluetoothEnabled()}"
            )
            val notif =
                    NotificationTemplates.lackingThingsNotification(this.applicationContext, CHANNEL_ID)
            startForeground(NOTIFICATION_ID, notif)
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
        if (!hasLocationPermissions() || !isBluetoothEnabled()) {
            CentralLog.i(
                    TAG,
                    "location permission: ${hasLocationPermissions()} bluetooth: ${isBluetoothEnabled()}"
            )
            val notif =
                    NotificationTemplates.lackingThingsNotification(this.applicationContext, CHANNEL_ID)
            startForeground(NOTIFICATION_ID, notif)
            return
        }

        when (cmd) {
            Command.ACTION_START -> {
                setupService()
                actionStart()
                Utils.scheduleNextHealthCheck(this.applicationContext, healthCheckInterval)
                Utils.scheduleBMUpdateCheck(this.applicationContext, bmCheckInterval)
            }

            Command.ACTION_SCAN -> {
                actionScan()
            }

            Command.ACTION_ADVERTISE -> {
                actionAdvertise()
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

    private fun actionStop() {
        stopForeground(true)
        stopSelf()
        CentralLog.w(TAG, "Service Stopping")
    }

    private fun actionHealthCheck() {
        Utils.scheduleNextHealthCheck(this.applicationContext, healthCheckInterval)
        performHealthCheck()
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
                            setupCycles()
                        },
                        onFailure = {
                        }
                )
            } else if (Preference.isOnBoarded(this)) {
                setupCycles()
            }
        }
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

    private fun calcPhaseShift(min: Long, max: Long): Long {
        return (min + (Math.random() * (max - min))).toLong()
    }

    private fun actionScan() {
        if (Preference.isOnBoarded(this) && Utils.needToUpdate(this.applicationContext) || broadcastMessage == null) {
            //need to pull new BM
            UpdateBroadcastMessageAndPerformScanWithExponentialBackOff(awsClient, applicationContext, lifecycle).invoke(
                    params = null,
                    onSuccess = {
                        broadcastMessage = it.tempId
                        performScanAndScheduleNextScan()
                    },
                    onFailure = {
                    }
            )
        } else if (Preference.isOnBoarded(this)) {
            performScanAndScheduleNextScan()
        }
    }

    private fun actionAdvertise() {
        setupAdvertiser()

        if (isBluetoothEnabled()) {
            advertiser?.startAdvertising(advertisingDuration)
        } else {
            CentralLog.w(TAG, "Unable to start advertising, bluetooth is off")
        }

        commandHandler.scheduleNextAdvertise(advertisingDuration + advertisingGap)
    }

    private fun setupService() {
        streetPassServer =
                streetPassServer ?: StreetPassServer(this.applicationContext, serviceUUID)
        setupScanner()
        setupAdvertiser()
    }

    private fun setupScanner() {
        streetPassScanner = streetPassScanner ?: StreetPassScanner(
                this,
                serviceUUID,
                scanDuration
        )
    }

    private fun setupAdvertiser() {
        advertiser = advertiser ?: BLEAdvertiser(serviceUUID)
    }

    private fun setupCycles() {
        setupScanCycles()
        setupAdvertisingCycles()
    }

    private fun setupScanCycles() {
        actionScan()
    }

    private fun setupAdvertisingCycles() {
        actionAdvertise()
    }

    private fun performScanAndScheduleNextScan() {

        setupScanner()

        commandHandler.scheduleNextScan(
                scanDuration + calcPhaseShift(
                        minScanInterval,
                        maxScanInterval
                )
        )

        startScan()

    }

    private fun startScan() {

        if (isBluetoothEnabled()) {

            streetPassScanner?.let { scanner ->
                if (!scanner.isScanning()) {
                    scanner.startScan()
                } else {
                    CentralLog.e(TAG, "Already scanning!")
                }
            }
        } else {
            CentralLog.w(TAG, "Unable to start scan - bluetooth is off")
        }
    }

    private fun performHealthCheck() {

        CentralLog.i(TAG, "Performing self diagnosis")

        if (!hasLocationPermissions() || !isBluetoothEnabled() || !isBatteryOptimizationDisabled()) {
            CentralLog.i(TAG, "no location permission")
            val notif =
                    NotificationTemplates.lackingThingsNotification(this.applicationContext, CHANNEL_ID)
            startForeground(NOTIFICATION_ID, notif)
            return
        }

        startForeground(
                NOTIFICATION_ID,
                NotificationTemplates.getRunningNotification(
                        this.applicationContext,
                        CHANNEL_ID
                )
        )

        //ensure our service is there
        setupService()

        if (!commandHandler.hasScanScheduled()) {
            CentralLog.w(TAG, "Missing Scan Schedule - rectifying")
            setupScanCycles()
        } else {
            CentralLog.w(TAG, "Scan Schedule present")
        }

        if (!commandHandler.hasAdvertiseScheduled()) {
            CentralLog.w(TAG, "Missing Advertise Schedule - rectifying")
            setupAdvertisingCycles()
        } else {
            CentralLog.w(
                    TAG,
                    "Advertise Schedule present. Should be advertising?:  ${advertiser?.shouldBeAdvertising
                            ?: false}. Is Advertising?: ${advertiser?.isAdvertising ?: false}"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CentralLog.i(TAG, "BluetoothMonitoringService destroyed - tearing down")

        teardown()
        unregisterReceivers()

        worker?.terminateConnections()
        worker?.unregisterReceivers()

        job.cancel()

        if (mBound) {
            unbindService(connection)
            mBound = false
        }

        CentralLog.i(TAG, "BluetoothMonitoringService destroyed")
    }

    private fun registerReceivers() {
        val recordAvailableFilter = IntentFilter(ACTION_RECEIVED_STREETPASS)
        localBroadcastManager.registerReceiver(streetPassReceiver, recordAvailableFilter)

        val statusReceivedFilter = IntentFilter(ACTION_RECEIVED_STATUS)
        localBroadcastManager.registerReceiver(statusReceiver, statusReceivedFilter)

        val bluetoothStatusReceivedFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStatusReceiver, bluetoothStatusReceivedFilter)

        CentralLog.i(TAG, "Receivers registered")
    }

    private fun unregisterReceivers() {
        try {
            localBroadcastManager.unregisterReceiver(streetPassReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "streetPassReceiver is not registered?")
        }

        try {
            localBroadcastManager.unregisterReceiver(statusReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "statusReceiver is not registered?")
        }

        try {
            unregisterReceiver(bluetoothStatusReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "bluetoothStatusReceiver is not registered?")
        }
    }

    inner class BluetoothStatusReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val action = intent.action
                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {

                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_TURNING_OFF")
                            val notif = NotificationTemplates.lackingThingsNotification(
                                    this@BluetoothMonitoringService.applicationContext,
                                    CHANNEL_ID
                            )
                            startForeground(NOTIFICATION_ID, notif)
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
                        }
                    }
                }
            }
        }
    }

    inner class StreetPassReceiver : BroadcastReceiver() {

        private val TAG = "StreetPassReceiver"

        override fun onReceive(context: Context, intent: Intent) {

            if (ACTION_RECEIVED_STREETPASS == intent.action) {
                val connRecord: ConnectionRecord? = intent.getParcelableExtra(STREET_PASS)
                CentralLog.d(
                        TAG,
                        "StreetPass received: $connRecord"
                )

                if (connRecord != null && connRecord.msg.isNotEmpty()) {

                    if (mBound) {
                        val proximity = mService.proximity
                        val light = mService.light
                        CentralLog.d(
                                TAG,
                                "Sensor values just before saving StreetPassRecord: proximity=$proximity light=$light"
                        )
                    }

                    val remoteBlob: String = if (connRecord.version == VERSION_ONE) {
                        with(receiver = connRecord) {
                            val plainRecordByteArray = gson.toJson(StreetPassRecordDatabase.Companion.EncryptedRecord(
                                    peripheral.modelP, central.modelC, rssi, txPower, msg = msg))
                                    .toByteArray(Charsets.UTF_8)
                            Encryption.encryptPayload(plainRecordByteArray)
                        }
                    } else {
                        //For version after version 1, the message is already encrypted in msg and we can store it as remote BLOB
                        connRecord.msg
                    }
                    val localBlob: String = if (connRecord.version == VERSION_ONE) {
                        ENCRYPTED_EMPTY_DICT
                    } else {
                        with(receiver = connRecord) {
                            val modelP = if (DUMMY_DEVICE == peripheral.modelP) null else peripheral.modelP
                            val modelC = if (DUMMY_DEVICE == central.modelC) null else central.modelC
                            val rssi = if (rssi == DUMMY_RSSI) null else rssi
                            val txPower = if (txPower == DUMMY_TXPOWER) null else txPower
                            val plainLocalBlob = gson.toJson(LocalBlobV2(modelP, modelC, rssi, txPower))
                                    .toByteArray(Charsets.UTF_8)
                            Encryption.encryptPayload(plainLocalBlob)
                        }
                    }

                    val record = StreetPassRecord(
                            v = if (connRecord.version == 1) TracerApp.protocolVersion else (connRecord.version),
                            org = connRecord.org,
                            localBlob = localBlob,
                            remoteBlob = remoteBlob
                    )

                    launch {
                        CentralLog.d(
                                TAG,
                                "Coroutine - Saving StreetPassRecord: ${Utils.getDate(record.timestamp)} $record")

                        streetPassRecordStorage.saveRecord(record)
                    }
                }
            }
        }
    }

    inner class StatusReceiver : BroadcastReceiver() {
        private val TAG = "StatusReceiver"

        override fun onReceive(context: Context, intent: Intent) {

            if (ACTION_RECEIVED_STATUS == intent.action) {
                val statusRecord: Status = intent.getParcelableExtra(STATUS)
                CentralLog.d(TAG, "Status received: ${statusRecord.msg}")

                if (statusRecord.msg.isNotEmpty()) {
                    val statusRecord = StatusRecord(statusRecord.msg)
                    launch {
                        statusRecordStorage.saveRecord(statusRecord)
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
        const val DAILY_UPLOAD_NOTIFICATION_CODE = 13


        var broadcastMessage: String? = null

        const val scanDuration: Long = BuildConfig.SCAN_DURATION
        const val minScanInterval: Long = BuildConfig.MIN_SCAN_INTERVAL
        const val maxScanInterval: Long = BuildConfig.MAX_SCAN_INTERVAL

        const val advertisingDuration: Long = BuildConfig.ADVERTISING_DURATION
        const val advertisingGap: Long = BuildConfig.ADVERTISING_INTERVAL

        const val maxQueueTime: Long = BuildConfig.MAX_QUEUE_TIME
        const val bmCheckInterval: Long = BuildConfig.BM_CHECK_INTERVAL
        const val healthCheckInterval: Long = BuildConfig.HEALTH_CHECK_INTERVAL

        const val connectionTimeout: Long = BuildConfig.CONNECTION_TIMEOUT

        const val blacklistDuration: Long = BuildConfig.BLACKLIST_DURATION

    }


}
