//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//
package au.gov.health.covidsafe.sensor.ble

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import au.gov.health.covidsafe.app.TracerApp
import au.gov.health.covidsafe.bluetooth.gatt.WriteRequestPayload
import au.gov.health.covidsafe.factory.NetworkFactory
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.sensor.SensorDelegate
import au.gov.health.covidsafe.sensor.ble.filter.BLEDeviceFilter
import au.gov.health.covidsafe.sensor.data.ConcreteSensorLogger
import au.gov.health.covidsafe.sensor.data.SensorLogger
import au.gov.health.covidsafe.sensor.datatype.*
import au.gov.health.covidsafe.streetpass.StreetPassPairingFix
import au.gov.health.covidsafe.streetpass.persistence.Encryption
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class ConcreteBLEReceiver(private val context: Context, private val bluetoothStateManager: BluetoothStateManager, timer: BLETimer, private val database: BLEDatabase, private val transmitter: BLETransmitter) : BluetoothGattCallback(), BLEReceiver, CoroutineScope {
    private val logger: SensorLogger = ConcreteSensorLogger("Sensor", "BLE.ConcreteBLEReceiver")
    private val operationQueue = Executors.newSingleThreadExecutor()
    private val scanResults: Queue<ScanResult> = ConcurrentLinkedQueue()
    private val awsClient = NetworkFactory.awsClient
    private val deviceFilter = BLEDeviceFilter()

    private enum class NextTask {
        nothing, readPayload, writePayload, writeRSSI, writePayloadSharing
    }

    //Result come here
    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, scanResult: ScanResult) {
            logger.debug("onScanResult (result={})", scanResult)
            scanResults.add(scanResult)
            // Create or update device in database
            val device = database.device(scanResult)
            device.registerDiscovery()
            // Read RSSI from scan result
            device.rssi(RSSI(scanResult.rssi))
        }


        override fun onBatchScanResults(results: List<ScanResult>) {
            for (scanResult in results) {
                onScanResult(0, scanResult)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            logger.fault("onScanFailed (error={})", onScanFailedErrorCodeToString(errorCode))
            super.onScanFailed(errorCode)
        }
    }

    // MARK:- BLEReceiver
    override fun add(delegate: SensorDelegate) {
        BLEReceiver.delegates.add(delegate)
    }

    override fun start() {
        logger.debug("start")
        // scanLoop is started by Bluetooth state
    }

    override fun stop() {
        logger.debug("stop")
        // scanLoop is stopped by Bluetooth state
    }

    // MARK:- Scan loop for startScan-wait-stopScan-processScanResults-wait-repeat
    private enum class ScanLoopState {
        scanStarting, scanStarted, scanStopping, scanStopped, processing, processed
    }

    private inner class ScanLoopTask : BLETimerDelegate {
        private var scanLoopState = ScanLoopState.processed
        private var lastStateChangeAt = System.currentTimeMillis()
        private fun state(now: Long, state: ScanLoopState) {
            val elapsed = now - lastStateChangeAt
            logger.debug("scanLoopTask, state change (from={},to={},elapsed={}ms)", scanLoopState, state, elapsed)
            scanLoopState = state
            lastStateChangeAt = now
        }

        private fun timeSincelastStateChange(now: Long): Long {
            return now - lastStateChangeAt
        }

        private fun bluetoothLeScanner(): BluetoothLeScanner? {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                logger.fault("ScanLoop denied, Bluetooth adapter unavailable")
                return null
            }
            val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            if (bluetoothLeScanner == null) {
                logger.fault("ScanLoop denied, Bluetooth LE scanner unavailable")
                return null
            }
            return bluetoothLeScanner
        }

        override fun bleTimer(now: Long) {
            when (scanLoopState) {
                ScanLoopState.processed -> {
                    if (bluetoothStateManager.state() == BluetoothState.poweredOn) {
                        val period = timeSincelastStateChange(now)
                        if (period >= scanOffDurationMillis) {
                            logger.debug("scanLoopTask, start scan (process={}ms)", period)
                            val bluetoothLeScanner = bluetoothLeScanner()
                            if (bluetoothLeScanner == null) {
                                logger.fault("scanLoopTask, start scan denied, Bluetooth LE scanner unavailable")
                                return
                            }
                            state(now, ScanLoopState.scanStarting)
                            startScan(bluetoothLeScanner, Callback<Boolean?> { value -> value?.let { state(now, if (value) ScanLoopState.scanStarted else ScanLoopState.scanStopped) } })
                        }
                    }
                    return
                }
                ScanLoopState.scanStarted -> {
                    val period = timeSincelastStateChange(now)
                    if (period >= scanOnDurationMillis) {
                        logger.debug("scanLoopTask, stop scan (scan={}ms)", period)
                        val bluetoothLeScanner = bluetoothLeScanner()
                        if (bluetoothLeScanner == null) {
                            logger.fault("scanLoopTask, stop scan denied, Bluetooth LE scanner unavailable")
                            return
                        }
                        state(now, ScanLoopState.scanStopping)
                        stopScan(bluetoothLeScanner, Callback<Boolean?> { value -> value?.let { state(now, ScanLoopState.scanStopped) } })
                    }
                    return
                }
                ScanLoopState.scanStopped -> {
                    if (bluetoothStateManager.state() == BluetoothState.poweredOn) {
                        val period = timeSincelastStateChange(now)
                        if (period >= scanRestDurationMillis) {
                            logger.debug("scanLoopTask, start processing (stop={}ms)", period)
                            state(now, ScanLoopState.processing)
                            processScanResults(object : Callback<Boolean?> {
                                override fun accept(value: Boolean?) {
                                    value?.let { state(now, ScanLoopState.processed) }
                                }
                            })
                        }
                    }
                    return
                }
            }
        }
    }

    /// Get BLE scanner and start scan
    private fun startScan(bluetoothLeScanner: BluetoothLeScanner, callback: Callback<Boolean?>?) {
        logger.debug("startScan")
        operationQueue.execute {
            try {
                scanForPeripherals(bluetoothLeScanner)
                logger.debug("startScan successful")
                callback?.let { callback.accept(true) }
            } catch (e: Throwable) {
                logger.fault("startScan failed", e)
                callback?.let { callback.accept(false) }
            }
        }
    }

    /// Scan for devices advertising sensor service and all Apple devices as
    // iOS background advert does not include service UUID. There is a risk
    // that the sensor will spend time communicating with Apple devices that
    // are not running the sensor code repeatedly, but there is no reliable
    // way of filtering this as the service may be absent only because of
    // transient issues. This will be handled in taskConnect.
    private fun scanForPeripherals(bluetoothLeScanner: BluetoothLeScanner) {
        logger.debug("scanForPeripherals")
        val filter: MutableList<ScanFilter> = ArrayList(2)
        filter.add(ScanFilter.Builder().setManufacturerData(
                BLESensorConfiguration.manufacturerIdForApple, ByteArray(0), ByteArray(0)).build())
        filter.add(ScanFilter.Builder().setServiceUuid(
                ParcelUuid(BLESensorConfiguration.serviceUUID),
                ParcelUuid(UUID(-0x1L, 0)))
                .build())
        val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()
        bluetoothLeScanner.startScan(filter, settings, scanCallback)
    }

    private fun processScanResults(callback: Callback<Boolean?>) {
        logger.debug("processScanResults")
        operationQueue.execute {
            try {
                processScanResults()
                logger.debug("processScanResults, processed scan results")
            } catch (e: Throwable) {
                logger.fault("processScanResults warning, processScanResults error", e)
                callback.accept(false)
            }
            logger.debug("processScanResults successful")
            callback.accept(true)
        }
    }

    /// Get BLE scanner and stop scan
    private fun stopScan(bluetoothLeScanner: BluetoothLeScanner, callback: Callback<Boolean?>) {
        logger.debug("stopScan")
        operationQueue.execute {
            try {
                bluetoothLeScanner.stopScan(scanCallback)
                logger.debug("stopScan, stopped scanner")
            } catch (e: Throwable) {
                logger.fault("stopScan warning, bluetoothLeScanner.stopScan error", e)
            }
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                bluetoothAdapter?.cancelDiscovery()
                logger.debug("stopScan, cancelled discovery")
            } catch (e: Throwable) {
                logger.fault("stopScan warning, bluetoothAdapter.cancelDiscovery error", e)
            }
            logger.debug("stopScan successful")
            callback.accept(true)
        }
    }

    // MARK:- Process scan results
    /// Process scan results.
    private fun processScanResults() {
        val t0 = System.currentTimeMillis()
        logger.debug("processScanResults (results={})", scanResults.size)
        // Identify devices discovered in last scan
        val didDiscover = didDiscover()
        taskRemoveExpiredDevices()
        taskCorrectConnectionStatus()
        taskConnect(didDiscover)
        val t1 = System.currentTimeMillis()
        logger.debug("processScanResults (results={},devices={},elapsed={}ms)", scanResults.size, didDiscover.size, t1 - t0)
    }
    // MARK:- didDiscover
    /**
     * Process scan results to ...
     * 1. Create BLEDevice from scan result for new devices
     * 2. Read RSSI
     * 3. Identify operating system where possible
     */
    private fun didDiscover(): List<BLEDevice> {
        // Take current copy of concurrently modifiable scan results
        val scanResultList: MutableList<ScanResult> = ArrayList(scanResults.size)
        while (scanResults.size > 0) {
            scanResultList.add(scanResults.poll())
        }

        // Process scan results and return devices created/updated in scan results
        logger.debug("didDiscover (scanResults={})", scanResultList.size)
        val deviceSet: MutableSet<BLEDevice> = HashSet()
        val devices: MutableList<BLEDevice> = ArrayList()
        for (scanResult in scanResultList) {
            val device = database.device(scanResult)
            if (deviceSet.add(device)) {
                logger.debug("didDiscover (device={})", device)
                devices.add(device)
            }
            // Set scan record
            device.scanRecord(scanResult.scanRecord)
            // Set TX power level
            if (device.scanRecord() != null) {
                val txPowerLevel = device.scanRecord().txPowerLevel
                if (txPowerLevel != Int.MIN_VALUE) {
                    device.txPower(BLE_TxPower(txPowerLevel))
                }
            }
            // Identify operating system from scan record where possible
            // - Sensor service found + Manufacturer is Apple -> iOS (Foreground)
            // - Sensor service found + Manufacturer not Apple -> Android
            // - Sensor service not found + Manufacturer is Apple -> iOS (Background) or Apple device not advertising sensor service, to be resolved later
            // - Sensor service not found + Manufacturer not Apple -> Ignore (shouldn't be possible as we are scanning for Apple or with service)
            val hasSensorService = hasSensorService(scanResult)
            val isAppleDevice = isAppleDevice(scanResult)
            if (hasSensorService && isAppleDevice) {
                // Definitely iOS device offering sensor service in foreground mode
                device.operatingSystem(BLEDeviceOperatingSystem.ios)
            } else if (hasSensorService) { // !isAppleDevice implied
                // Definitely Android device offering sensor service
                if (device.operatingSystem() != BLEDeviceOperatingSystem.android) {
                    device.operatingSystem(BLEDeviceOperatingSystem.android_tbc)
                }
            } else if (isAppleDevice) { // !hasSensorService implied
                // Filter device by advert messages unless it is already confirmed ios device
                val matchingPattern: BLEDeviceFilter.MatchingPattern? = deviceFilter.match(device)
                if (device.operatingSystem() !== BLEDeviceOperatingSystem.ios && matchingPattern != null) {
                    logger.fault("didDiscover, ignoring filtered device (device={},pattern={},message={})", device, matchingPattern.filterPattern.regularExpression, matchingPattern.message)
                    device.operatingSystem(BLEDeviceOperatingSystem.ignore)
                }
                // Possibly an iOS device offering sensor service in background mode,
                // can't be sure without additional checks after connection, so
                // only set operating system if it is unknown to offer a guess.
                if (device.operatingSystem() == BLEDeviceOperatingSystem.unknown) {
                    device.operatingSystem(BLEDeviceOperatingSystem.ios_tbc)
                }
            } else {
                // Sensor service not found + Manufacturer not Apple should be impossible
                // as we are scanning for devices with sensor service or Apple device.
                logger.fault("didDiscover, invalid non-Apple device without sensor service (device={})", device)
                if (!(device.operatingSystem() == BLEDeviceOperatingSystem.ios || device.operatingSystem() == BLEDeviceOperatingSystem.android)) {
                    device.operatingSystem(BLEDeviceOperatingSystem.ignore)
                }
            }
        }
        return devices
    }

    // MARK:- House keeping tasks
    /// Remove devices that have not been updated for over 15 minutes, as the UUID
    // is likely to have changed after being out of range for over 20 minutes,
    // so it will require discovery. Discovery is fast and cheap on Android.
    private fun taskRemoveExpiredDevices() {
        val devicesToRemove: MutableList<BLEDevice> = ArrayList()
        for (device in database.devices()) {
            if (device.timeIntervalSinceLastUpdate().value > TimeInterval.minutes(15).value) {
                devicesToRemove.add(device)
            }
        }
        for (device in devicesToRemove) {
            logger.debug("taskRemoveExpiredDevices (remove={})", device)
            database.delete(device.identifier)
        }
    }

    /// Connections should not be held for more than 1 minute, likely to have not received onConnectionStateChange callback.
    private fun taskCorrectConnectionStatus() {
        for (device in database.devices()) {
            if (device.state() == BLEDeviceState.connected && device.timeIntervalSinceConnected().value > TimeInterval.minute.value) {
                logger.debug("taskCorrectConnectionStatus (device={})", device)
                device.state(BLEDeviceState.disconnected)
            }
        }
    }

    // MARK:- Connect task
    private fun taskConnect(discovered: List<BLEDevice>) {
        // Clever connection prioritisation is pointless here as devices
        // like the Samsung A10 and A20 changes mac address on every scan
        // call, so optimising new device handling is more effective.
        val timeStart = System.currentTimeMillis()
        var devicesProcessed = 0
        for (device in discovered) {
            // Stop process if exceeded time limit
            val elapsedTime = System.currentTimeMillis() - timeStart
            if (elapsedTime >= scanProcessDurationMillis) {
                logger.debug("taskConnect, reached time limit (elapsed={}ms,limit={}ms)", elapsedTime, scanProcessDurationMillis)
                break
            }
            if (devicesProcessed > 0) {
                val predictedElapsedTime = Math.round(elapsedTime / devicesProcessed.toDouble() * (devicesProcessed + 1))
                if (predictedElapsedTime > scanProcessDurationMillis) {
                    logger.debug("taskConnect, likely to exceed time limit soon (elapsed={}ms,devicesProcessed={},predicted={}ms,limit={}ms)", elapsedTime, devicesProcessed, predictedElapsedTime, scanProcessDurationMillis)
                    break
                }
            }
            if (nextTaskForDevice(device) == NextTask.nothing) {
                logger.debug("taskConnect, no pending action (device={})", device)
                continue
            }
            taskConnectDevice(device)
            devicesProcessed++
        }
    }

    private fun taskConnectDevice(device: BLEDevice) {
        if (device.state() == BLEDeviceState.connected) {
            logger.debug("taskConnectDevice, already connected to transmitter (device={})", device)
            return
        }
        // Connect (timeout at 95% = 2 SD)
        val timeConnect = System.currentTimeMillis()
        logger.debug("taskConnectDevice, connect (device={})", device)
        device.state(BLEDeviceState.connecting)
        //val gatt = device.peripheral().connectGatt(context, false, this)
        val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.peripheral().connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE)
        } else {
            // use reflection to call connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback, int transport)
            try {
                device.javaClass.getMethod(
                        "connectGatt", Context::class.java, Boolean::class.java, BluetoothGattCallback::class.java, Int::class.java
                ).invoke(
                        // BluetoothDevice.TRANSPORT_LE = 2
                        device, context, false, this, 2 ) as BluetoothGatt
            } catch (e: Exception) {
                logger.fault("Reflection call of connectGatt() failed.", e)
                // reflection failed; call connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback) instead
                device.peripheral().connectGatt(context, false, this)
            }
        }
        if (gatt == null) {
            logger.fault("taskConnectDevice, connect failed (device={})", device)
            device.state(BLEDeviceState.disconnected)
            return
        }
        // Wait for connection
        while (device.state() != BLEDeviceState.connected && device.state() != BLEDeviceState.disconnected && System.currentTimeMillis() - timeConnect < timeToConnectDeviceLimitMillis) {
            try {
                Thread.sleep(200)
            } catch (e: Throwable) {
                logger.fault("Timer interrupted", e)
            }
        }
        if (device.state() != BLEDeviceState.connected) {
            logger.fault("taskConnectDevice, connect timeout (device={})", device)
            try {
                gatt.close()
            } catch (e: Throwable) {
                logger.fault("taskConnectDevice, close failed (device={})", device, e)
            }
            return
        } else {
            val connectElapsed = System.currentTimeMillis() - timeConnect
            // Add sample to adaptive connection timeout
            timeToConnectDevice.add(connectElapsed.toDouble())
            logger.debug("taskConnectDevice, connected (device={},elapsed={}ms,statistics={})", device, connectElapsed, timeToConnectDevice)
        }
        // Wait for disconnection
        while (device.state() != BLEDeviceState.disconnected && System.currentTimeMillis() - timeConnect < scanProcessDurationMillis) {
            try {
                Thread.sleep(500)
            } catch (e: Throwable) {
                logger.fault("Timer interrupted", e)
            }
        }
        var success = true
        // Timeout connection if required, and always set state to disconnected
        if (device.state() != BLEDeviceState.disconnected) {
            logger.fault("taskConnectDevice, disconnect timeout (device={})", device)
            try {
                gatt.close()
            } catch (e: Throwable) {
                logger.fault("taskConnectDevice, close failed (device={})", device, e)
            }
            success = false
        }
        device.state(BLEDeviceState.disconnected)
        val timeDisconnect = System.currentTimeMillis()
        val timeElapsed = timeDisconnect - timeConnect
        if (success) {
            timeToProcessDevice.add(timeElapsed.toDouble())
            logger.debug("taskConnectDevice, complete (success=true,device={},elapsed={}ms,statistics={})", device, timeElapsed, timeToProcessDevice)
        } else {
            logger.fault("taskConnectDevice, complete (success=false,device={},elapsed={}ms)", device, timeElapsed)
        }
    }

    // MARK:- BluetoothStateManagerDelegate
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        val device = database.device(gatt.device)
        logger.debug("onConnectionStateChange (device={},status={},state={})", device, bleStatus(status), bleState(newState))
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            device.state(BLEDeviceState.connected)
            gatt.discoverServices()
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            gatt.close()
            device.state(BLEDeviceState.disconnected)
            if (status != 0) {
                if (!(device.operatingSystem() == BLEDeviceOperatingSystem.ios || device.operatingSystem() == BLEDeviceOperatingSystem.android)) {
                    device.operatingSystem(BLEDeviceOperatingSystem.ignore)
                }
            }
        } else {
            logger.debug("onConnectionStateChange (device={},status={},state={})", device, bleStatus(status), bleState(newState))
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        val device = database.device(gatt.device)
        logger.debug("onServicesDiscovered (device={},status={})", device, bleStatus(status))
        val service = gatt.getService(BLESensorConfiguration.serviceUUID)
        if (service == null) {
            logger.fault("onServicesDiscovered, missing sensor service (device={})", device)
            // Ignore device for a while unless it is a confirmed iOS or Android device
            if (!(device.operatingSystem() == BLEDeviceOperatingSystem.ios || device.operatingSystem() == BLEDeviceOperatingSystem.android)) {
                device.operatingSystem(BLEDeviceOperatingSystem.ignore)
            }
            gatt.disconnect()
            return
        }
        logger.debug("onServicesDiscovered, found sensor service (device={})", device)
        device.invalidateCharacteristics()
        var readService: Boolean = false
        for (characteristic in service.characteristics) {
            // Confirm operating system with signal characteristic
            if (characteristic.uuid == BLESensorConfiguration.androidSignalCharacteristicUUID) {
                logger.debug("onServicesDiscovered, found Android signal characteristic (device={})", device)
                device.operatingSystem(BLEDeviceOperatingSystem.android)
                device.signalCharacteristic(characteristic)
            } else if (characteristic.uuid == BLESensorConfiguration.iosSignalCharacteristicUUID) {
                logger.debug("onServicesDiscovered, found iOS signal characteristic (device={})", device)
                device.operatingSystem(BLEDeviceOperatingSystem.ios)
                device.signalCharacteristic(characteristic)
            } else if (characteristic.uuid == BLESensorConfiguration.payloadCharacteristicUUID) {
                logger.debug("onServicesDiscovered, found payload characteristic (device={})", device)
                device.payloadCharacteristic(characteristic)
                readService = true
            } else if (characteristic.uuid == BLESensorConfiguration.legacyCovidsafePayloadCharacteristicUUID && !readService) {
                logger.debug("onServicesDiscovered, found covidsafe legacy payload characteristic (device={})", device)
                //If they have the legacy characteristic we know it a COVID app and can set the OS to be confirmed
                if(device.operatingSystem() == BLEDeviceOperatingSystem.android_tbc) {
                    device.operatingSystem(BLEDeviceOperatingSystem.android)
                }else if(device.operatingSystem() == BLEDeviceOperatingSystem.ios_tbc) {
                    device.operatingSystem(BLEDeviceOperatingSystem.ios)
                }
                device.payloadCharacteristic(characteristic)
                device.legacyPayloadCharacteristic(characteristic)
            }
        }
        nextTask(gatt)
    }

    private fun nextTaskForDevice(device: BLEDevice): NextTask {
        // No task for devices marked as .ignore
        if (device.ignore()) {
            return NextTask.nothing
        }
        // If marked as ignore but ignore has expired, change to unknown
        if (device.operatingSystem() == BLEDeviceOperatingSystem.ignore) {
            logger.debug("nextTaskForDevice, switching ignore to unknown (device={},reason=ignoreExpired)", device)
            device.operatingSystem(BLEDeviceOperatingSystem.unknown)
        }
        // No task for devices marked as receive only (no advert to connect to)
        if (device.receiveOnly()) {
            return NextTask.nothing
        }
        // Resolve or confirm operating system by reading payload which
        // triggers characteristic discovery to confirm the operating system
        if (device.operatingSystem() == BLEDeviceOperatingSystem.unknown ||
                device.operatingSystem() == BLEDeviceOperatingSystem.ios_tbc) {
            logger.debug("nextTaskForDevice (device={},task=readPayload|OS)", device)
            return NextTask.readPayload
        }
        // Get payload as top priority
        if (device.payloadData() == null) {
            logger.debug("nextTaskForDevice (device={},task=readPayload)", device)
            return NextTask.readPayload
        }
        if (device.timeIntervalSinceLastPayloadUpdate().millis() > payloadDataUpdateTimeInterval) {
            logger.debug("nextTaskForDevice (device={},task=readPayloadUpdate)", device)
            return NextTask.readPayload
        }

        // Write payload, rssi and payload sharing data if this device cannot transmit
        if (!transmitter.isSupported) {
            // Write payload data as top priority
            if (device.timeIntervalSinceLastWritePayload().value > TimeInterval.minutes(5).value) {
                logger.debug("nextTaskForDevice (device={},task=writePayload,elapsed={})", device, device.timeIntervalSinceLastWritePayload())
                return NextTask.writePayload
            }
            // Write payload sharing data to iOS device if there is data to be shared (alternate between payload sharing and write RSSI)
            val payloadSharingData = database.payloadSharingData(device)
            if (device.operatingSystem() == BLEDeviceOperatingSystem.ios
                    && payloadSharingData.data.value.size > 0
                    && device.timeIntervalSinceLastWritePayloadSharing().value >= TimeInterval.seconds(15).value
                    && device.timeIntervalSinceLastWritePayloadSharing().value >= device.timeIntervalSinceLastWriteRssi().value) {
                logger.debug("nextTaskForDevice (device={},task=writePayloadSharing,dataLength={},elapsed={})", device, payloadSharingData.data.value.size,
                        device.timeIntervalSinceLastWritePayloadSharing())
                return NextTask.writePayloadSharing
            }
            // Write RSSI as frequently as reasonable
            if (device.rssi() != null
                    && device.timeIntervalSinceLastWriteRssi().value >= TimeInterval.seconds(15).value
                    && (device.timeIntervalSinceLastWritePayload().millis() < payloadDataUpdateTimeInterval
                            || device.timeIntervalSinceLastWriteRssi().value >= device.timeIntervalSinceLastWritePayload().value)) {
                logger.debug("nextTaskForDevice (device={},task=writeRSSI,elapsed={})", device, device.timeIntervalSinceLastWriteRssi())
                return NextTask.writeRSSI
            }
            // Write payload update if required
            if (device.timeIntervalSinceLastWritePayload().millis() > payloadDataUpdateTimeInterval) {
                logger.debug("nextTaskForDevice (device={},task=writePayloadUpdate,elapsed={})", device, device.timeIntervalSinceLastWritePayload());
                return NextTask.writePayload;
            }
        }
        // Write payload sharing data to iOS
        if (device.operatingSystem() == BLEDeviceOperatingSystem.ios) {
            // Write payload sharing data to iOS device if there is data to be shared
            val payloadSharingData = database.payloadSharingData(device)
            if (device.operatingSystem() == BLEDeviceOperatingSystem.ios && payloadSharingData.data.value.size > 0 && device.timeIntervalSinceLastWritePayloadSharing().value >= TimeInterval.seconds(15).value) {
                logger.debug("nextTaskForDevice (device={},task=writePayloadSharing,dataLength={},elapsed={})", device, payloadSharingData.data.value.size, device.timeIntervalSinceLastWritePayloadSharing())
                return NextTask.writePayloadSharing
            }
        }
        return NextTask.nothing
    }

    private fun nextTask(gatt: BluetoothGatt) {
        val device = database.device(gatt.device)
        val nextTask = nextTaskForDevice(device)
        when (nextTask) {
            NextTask.readPayload -> {
                val payloadCharacteristic = device.payloadCharacteristic()

                if (payloadCharacteristic == null) {
                    logger.fault("nextTask failed (task=readPayload,device={},reason=missingPayloadCharacteristic)", device)
                    gatt.disconnect()
                    return  // => onConnectionStateChange
                }
                StreetPassPairingFix.bypassAuthenticationRetry(gatt);
                if (!gatt.readCharacteristic(payloadCharacteristic)) {
                    logger.fault("nextTask failed (task=readPayload,device={},reason=readCharacteristicFailed)", device)
                    gatt.disconnect()
                    return  // => onConnectionStateChange
                }
                logger.debug("nextTask (task=readPayload,device={})", device)
                return  // => onCharacteristicRead | timeout
            }
            NextTask.writePayload -> {
                val payloadData = transmitter.payloadData()
                if (payloadData == null || payloadData.value == null || payloadData.value.size == 0) {
                    logger.fault("nextTask failed (task=writePayload,device={},reason=missingPayloadData)", device)
                    gatt.disconnect()
                    return  // => onConnectionStateChange
                }
                val data = SignalCharacteristicData.encodeWritePayload(transmitter.payloadData())
                logger.debug("nextTask (task=writePayload,device={},dataLength={})", device, data.value.size)
                writeSignalCharacteristic(gatt, NextTask.writePayload, data.value)
                return
            }
            NextTask.writePayloadSharing -> {
                val payloadSharingData = database.payloadSharingData(device)
                if (payloadSharingData == null) {
                    logger.fault("nextTask failed (task=writePayloadSharing,device={},reason=missingPayloadSharingData)", device)
                    gatt.disconnect()
                    return
                }
                val data = SignalCharacteristicData.encodeWritePayloadSharing(payloadSharingData)
                logger.debug("nextTask (task=writePayloadSharing,device={},dataLength={})", device, data.value.size)
                writeSignalCharacteristic(gatt, NextTask.writePayloadSharing, data.value)
                return
            }
            NextTask.writeRSSI -> {
                val signalCharacteristic = device.signalCharacteristic()
                if (signalCharacteristic == null) {
                    logger.fault("nextTask failed (task=writeRSSI,device={},reason=missingSignalCharacteristic)", device)
                    gatt.disconnect()
                    return
                }
                val rssi = device.rssi()
                if (rssi == null) {
                    logger.fault("nextTask failed (task=writeRSSI,device={},reason=missingRssiData)", device)
                    gatt.disconnect()
                    return
                }
                val data = SignalCharacteristicData.encodeWriteRssi(rssi)
                logger.debug("nextTask (task=writeRSSI,device={},dataLength={})", device, data.value.size)
                writeSignalCharacteristic(gatt, NextTask.writeRSSI, data.value)
                return
            }
        }
        logger.debug("nextTask (task=nothing,device={})", device)
        gatt.disconnect()
    }

    inner class EncryptedWriteRequestPayload(val timestamp: Long, val modelC: String, val rssi: Int, val txPower: Int?, val msg: String?)

    private fun getWritePayloadForCentral(device: BLEDevice): WriteRequestPayload? {
        val thisCentralDevice = TracerApp.asCentralDevice()
        val gson = GsonBuilder().disableHtmlEscaping().create()

        val DUMMY_DEVICE = ""
        val DUMMY_RSSI = 999
        val DUMMY_TXPOWER = 999

        val rssi = if (device.rssi() != null) device.rssi().value else return null
        val txPower = if (device.txPower() != null) device.txPower().value else DUMMY_TXPOWER

        val plainRecord = gson.toJson(EncryptedWriteRequestPayload(
                System.currentTimeMillis() / 1000L,
                thisCentralDevice.modelC,
                rssi,
                txPower,
                TracerApp.thisDeviceMsg()))

        val remoteBlob = Encryption.encryptPayload(plainRecord.toByteArray(Charsets.UTF_8))

        val writedata = WriteRequestPayload(
                v = TracerApp.protocolVersion,
                msg = remoteBlob,
                org = TracerApp.ORG,
                modelC = DUMMY_DEVICE,
                rssi = DUMMY_RSSI,
                txPower = DUMMY_TXPOWER
        )

        return writedata
    }

    private fun writeSignalCharacteristic(gatt: BluetoothGatt, task: NextTask, data: ByteArray?) {
        val device = database.device(gatt.device)
        val signalCharacteristic = device.signalCharacteristic()
        if (signalCharacteristic == null) {
            logger.fault("writeSignalCharacteristic failed (task={},device={},reason=missingSignalCharacteristic)", task, device)
            gatt.disconnect()
            return
        }
        if (data == null || data.size == 0) {
            logger.fault("writeSignalCharacteristic failed (task={},device={},reason=missingData)", task, device)
            gatt.disconnect()
            return
        }
        if (signalCharacteristic.uuid == BLESensorConfiguration.iosSignalCharacteristicUUID) {
            device.signalCharacteristicWriteValue = data
            device.signalCharacteristicWriteQueue = null
            signalCharacteristic.value = data
            signalCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            StreetPassPairingFix.bypassAuthenticationRetry(gatt)
            if (!gatt.writeCharacteristic(signalCharacteristic)) {
                logger.fault("writeSignalCharacteristic to iOS failed (task={}},device={},reason=writeCharacteristicFailed)", task, device)
                gatt.disconnect()
            } else {
                logger.debug("writeSignalCharacteristic to iOS (task={},dataLength={},device={})", task, data.size, device)
                // => onCharacteristicWrite
            }
            return
        }
        if (signalCharacteristic.uuid == BLESensorConfiguration.androidSignalCharacteristicUUID) {
            device.signalCharacteristicWriteValue = data
            device.signalCharacteristicWriteQueue = fragmentDataByMtu(data)
            if (writeAndroidSignalCharacteristic(gatt) == WriteAndroidSignalCharacteristicResult.failed) {
                logger.fault("writeSignalCharacteristic to Android failed (task={}},device={},reason=writeCharacteristicFailed)", task, device)
                gatt.disconnect()
            } else {
                logger.debug("writeSignalCharacteristic to Android (task={},dataLength={},device={})", task, data.size, device)
                // => onCharacteristicWrite
            }
        }
    }

    private enum class WriteAndroidSignalCharacteristicResult {
        moreToWrite, complete, failed
    }

    private fun writeAndroidSignalCharacteristic(gatt: BluetoothGatt): WriteAndroidSignalCharacteristicResult {
        val device = database.device(gatt.device)
        val signalCharacteristic = device.signalCharacteristic()
        if (signalCharacteristic == null) {
            logger.fault("writeAndroidSignalCharacteristic failed (device={},reason=missingSignalCharacteristic)", device)
            return WriteAndroidSignalCharacteristicResult.failed
        }
        if (device.signalCharacteristicWriteQueue == null || device.signalCharacteristicWriteQueue.size == 0) {
            logger.debug("writeAndroidSignalCharacteristic completed (device={})", device)
            return WriteAndroidSignalCharacteristicResult.complete
        }
        logger.debug("writeAndroidSignalCharacteristic (device={},queue={})", device, device.signalCharacteristicWriteQueue.size)
        val data = device.signalCharacteristicWriteQueue.poll()
        signalCharacteristic.value = data
        signalCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        StreetPassPairingFix.bypassAuthenticationRetry(gatt)
        return if (!gatt.writeCharacteristic(signalCharacteristic)) {
            logger.fault("writeAndroidSignalCharacteristic failed (device={},reason=writeCharacteristicFailed)", device)
            WriteAndroidSignalCharacteristicResult.failed
        } else {
            logger.debug("writeAndroidSignalCharacteristic (device={},remaining={})", device, device.signalCharacteristicWriteQueue.size)
            WriteAndroidSignalCharacteristicResult.moreToWrite
        }
    }

    /// Split data into fragments, where each fragment has length <= mtu
    private fun fragmentDataByMtu(data: ByteArray): Queue<ByteArray> {
        val fragments: Queue<ByteArray> = ConcurrentLinkedQueue()
        var i = 0
        while (i < data.size) {
            val fragment = ByteArray(Math.min(defaultMTU, data.size - i))
            System.arraycopy(data, i, fragment, 0, fragment.size)
            fragments.add(fragment)
            i += defaultMTU
        }
        return fragments
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        val device = database.device(gatt.device)
        val success = status == BluetoothGatt.GATT_SUCCESS
        logger.debug("onCharacteristicRead (device={},status={})", device, bleStatus(status))
        if (characteristic.uuid == BLESensorConfiguration.payloadCharacteristicUUID || characteristic.uuid == BLESensorConfiguration.legacyCovidsafePayloadCharacteristicUUID) {
            val payloadData = if (characteristic.value != null) PayloadData(characteristic.value) else null
            if (success) {
                if (payloadData != null) {
                    logger.debug("onCharacteristicRead, read payload data success (device={},payload={})", device, payloadData.shortName())
                    device.payloadData(payloadData)
                } else {
                    logger.fault("onCharacteristicRead, read payload data failed, no data (device={})", device)
                }
            } else {
                logger.fault("onCharacteristicRead, read payload data failed (device={})", device)
            }
        }
        nextTask(gatt)
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        val device = database.device(gatt.device)
        logger.debug("onCharacteristicWrite (device={},status={})", device, bleStatus(status))
        val signalCharacteristic = device.signalCharacteristic()
        val success = status == BluetoothGatt.GATT_SUCCESS
        if (signalCharacteristic.uuid == BLESensorConfiguration.androidSignalCharacteristicUUID) {
            if (success && writeAndroidSignalCharacteristic(gatt) == WriteAndroidSignalCharacteristicResult.moreToWrite) {
                return
            }
        }
        val signalCharacteristicDataType = SignalCharacteristicData.detect(Data(device.signalCharacteristicWriteValue))
        signalCharacteristic.value = ByteArray(0)
        device.signalCharacteristicWriteValue = null
        device.signalCharacteristicWriteQueue = null
        when (signalCharacteristicDataType) {
            SignalCharacteristicDataType.payload -> {
                if (success) {
                    logger.debug("onCharacteristicWrite, write payload success (device={})", device)
                    device.registerWritePayload()
                } else {
                    logger.fault("onCharacteristicWrite, write payload failed (device={})", device)
                }
                return
            }
            SignalCharacteristicDataType.rssi -> {
                if (success) {
                    logger.debug("onCharacteristicWrite, write RSSI success (device={})", device)
                    device.registerWriteRssi()
                } else {
                    logger.fault("onCharacteristicWrite, write RSSI failed (device={})", device)
                }
                return
            }
            SignalCharacteristicDataType.payloadSharing -> {
                if (success) {
                    logger.debug("onCharacteristicWrite, write payload sharing success (device={})", device)
                    device.registerWritePayloadSharing()
                } else {
                    logger.fault("onCharacteristicWrite, write payload sharing failed (device={})", device)
                }
                return
            }
            else -> {
                logger.fault("onCharacteristicWrite, write unknown data (device={},success={})", device, success)
                return
            }
        }
        nextTask(gatt)
    }

    companion object {
        // Scan ON/OFF/PROCESS durations
        private val scanOnDurationMillis = TimeInterval.seconds(4).millis()
        private val scanRestDurationMillis = TimeInterval.seconds(1).millis()
        private val scanProcessDurationMillis = TimeInterval.seconds(60).millis()
        private val scanOffDurationMillis = TimeInterval.seconds(2).millis()
        private val timeToConnectDeviceLimitMillis = TimeInterval.seconds(12).millis()
        private val timeToConnectDevice = Sample()
        private val timeToProcessDevice = Sample()
        private const val defaultMTU = 20
        private val payloadDataUpdateTimeInterval = TimeInterval.minutes(5).millis()

        /// Does scan result include advert for sensor service?
        private fun hasSensorService(scanResult: ScanResult): Boolean {
            val scanRecord = scanResult.scanRecord ?: return false
            val serviceUuids = scanRecord.serviceUuids
            if (serviceUuids == null || serviceUuids.size == 0) {
                return false
            }
            for (serviceUuid in serviceUuids) {
                if (serviceUuid.uuid == BLESensorConfiguration.serviceUUID) {
                    return true
                }
            }
            return false
        }

        /// Does scan result indicate device was manufactured by Apple?
        private fun isAppleDevice(scanResult: ScanResult): Boolean {
            val scanRecord = scanResult.scanRecord ?: return false
            val data = scanRecord.getManufacturerSpecificData(BLESensorConfiguration.manufacturerIdForApple)
            return data != null
        }

        // MARK:- Bluetooth code transformers
        private fun bleStatus(status: Int): String {
            return if (status == BluetoothGatt.GATT_SUCCESS) {
                "GATT_SUCCESS"
            } else {
                "GATT_FAILURE"
            }
        }

        private fun bleState(state: Int): String {
            return when (state) {
                BluetoothProfile.STATE_CONNECTED -> "STATE_CONNECTED"
                BluetoothProfile.STATE_DISCONNECTED -> "STATE_DISCONNECTED"
                else -> "UNKNOWN_STATE_$state"
            }
        }

        private fun onScanFailedErrorCodeToString(errorCode: Int): String {
            return when (errorCode) {
                ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "SCAN_FAILED_ALREADY_STARTED"
                ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED"
                ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "SCAN_FAILED_INTERNAL_ERROR"
                ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "SCAN_FAILED_FEATURE_UNSUPPORTED"
                else -> "UNKNOWN_ERROR_CODE_$errorCode"
            }
        }
    }

    /**
     * Receiver starts automatically when Bluetooth is enabled.
     */
    init {
        timer.add(ScanLoopTask())
    }
    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
}