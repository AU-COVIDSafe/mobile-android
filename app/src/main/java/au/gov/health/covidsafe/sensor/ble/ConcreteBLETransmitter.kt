//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//
package au.gov.health.covidsafe.sensor.ble

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import au.gov.health.covidsafe.BuildConfig
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.sensor.SensorDelegate
import au.gov.health.covidsafe.sensor.data.ConcreteSensorLogger
import au.gov.health.covidsafe.sensor.data.SensorLogger
import au.gov.health.covidsafe.sensor.datatype.*
import au.gov.health.covidsafe.sensor.payload.PayloadDataSupplier
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference


class ConcreteBLETransmitter(
        private val context: Context,
        private val bluetoothStateManager: BluetoothStateManager,
        timer: BLETimer,
        private val payloadDataSupplier: PayloadDataSupplier,
        private val database: BLEDatabase) : BLETransmitter, BluetoothStateManagerDelegate {
    private val logger: SensorLogger = ConcreteSensorLogger("Sensor", "BLE.ConcreteBLETransmitter")
    private val operationQueue = Executors.newSingleThreadExecutor()

    // Referenced by startAdvert and stopExistingGattServer ONLY
    private var bluetoothGattServer: BluetoothGattServer? = null

    override fun add(delegate: SensorDelegate) {
        BLETransmitter.delegates.add(delegate)
    }

    override fun start() {
        logger.debug("start (supported={})", isSupported)
        // advertLoop is started by Bluetooth state
    }

    override fun stop() {
        logger.debug("stop")
        // advertLoop is stopped by Bluetooth state
    }

    // MARK:- Advert loop
    private enum class AdvertLoopState {
        starting, started, stopping, stopped
    }

    /// Get Bluetooth LE advertiser
    private fun bluetoothLeAdvertiser(): BluetoothLeAdvertiser? {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            logger.debug("bluetoothLeAdvertiser, no Bluetooth Adapter available")
            return null
        }
        val supported = bluetoothAdapter.isMultipleAdvertisementSupported
        return try {
            val bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
            if (bluetoothLeAdvertiser == null) {
                logger.debug("bluetoothLeAdvertiser, no LE advertiser present (multiSupported={}, exception=no)", supported)
                return null
            }
            // log this, as this will allow us to identify handsets with a different API implementation
            logger.debug("bluetoothLeAdvertiser, LE advertiser present (multiSupported={})", supported)
            bluetoothLeAdvertiser
        } catch (e: Exception) {
            // log it, as this will allow us to identify handsets with the expected API implementation (from Android API source code)
            logger.debug("bluetoothLeAdvertiser, no LE advertiser present (multiSupported={}, exception={})", supported, e.message)
            null
        }
    }

    private inner class AdvertLoopTask : BLETimerDelegate {
        private var advertLoopState = AdvertLoopState.stopped
        private var lastStateChangeAt = System.currentTimeMillis()
        private var advertiseCallback: AdvertiseCallback? = null

        private fun state(now: Long, state: AdvertLoopState) {
            val elapsed = now - lastStateChangeAt
            logger.debug("advertLoopTask, state change (from={},to={},elapsed={}ms)", advertLoopState, state, elapsed)
            advertLoopState = state
            lastStateChangeAt = now
        }

        private fun timeSincelastStateChange(now: Long): Long {
            return now - lastStateChangeAt
        }

        override fun bleTimer(now: Long) {
            if (!isSupported || bluetoothStateManager.state() == BluetoothState.poweredOff) {
                if (advertLoopState != AdvertLoopState.stopped) {
                    advertiseCallback = null
                    bluetoothGattServer = null
                    state(now, AdvertLoopState.stopped)
                    logger.debug("advertLoopTask, stop advert (advert={}ms)", timeSincelastStateChange(now))
                }
                return
            }
            when (advertLoopState) {
                AdvertLoopState.stopped -> {
                    if (bluetoothStateManager.state() == BluetoothState.poweredOn) {
                        val period = timeSincelastStateChange(now)
                        if (period >= advertOffDurationMillis) {
                            logger.debug("advertLoopTask, start advert (stop={}ms)", period)
                            val bluetoothLeAdvertiser = bluetoothLeAdvertiser()
                            if (bluetoothLeAdvertiser == null) {
                                logger.fault("advertLoopTask, start advert denied, Bluetooth LE advertiser unavailable")
                                return
                            }
                            state(now, AdvertLoopState.starting)
                            startAdvert(bluetoothLeAdvertiser, Callback { value ->
                                advertiseCallback = value.b
                                bluetoothGattServer = value.c
                                state(now, if (value.a) AdvertLoopState.started else AdvertLoopState.stopped)
                            })
                        }
                    }
                    return
                }
                AdvertLoopState.started -> {
                    val period = timeSincelastStateChange(now)
                    if (period >= BLESensorConfiguration.advertRefreshTimeInterval.millis()) {
                        logger.debug("advertLoopTask, stop advert (advert={}ms)", period)
                        val bluetoothLeAdvertiser = bluetoothLeAdvertiser()
                        if (bluetoothLeAdvertiser == null) {
                            logger.fault("advertLoopTask, stop advert denied, Bluetooth LE advertiser unavailable")
                            return
                        }
                        state(now, AdvertLoopState.stopping)
                        stopAdvert(bluetoothLeAdvertiser, advertiseCallback, bluetoothGattServer, object : Callback<Boolean> {
                            override fun accept(value: Boolean) {
                                advertiseCallback = null
                                bluetoothGattServer = null
                                state(now, AdvertLoopState.stopped)
                            }
                        })
                    }
                    return
                }
            }
        }
    }

    private fun stopExistingGattServer() {
        if (null != bluetoothGattServer) {
            // Stop old version, if there's already a proxy reference
            bluetoothGattServer = try {
                bluetoothGattServer!!.clearServices()
                bluetoothGattServer!!.close()
                null
            } catch (e2: Throwable) {
                logger.fault("stopGattServer failed to stop EXISTING GATT server", e2)
                null
            }
        }
    }

    // MARK:- Start and stop advert
    private fun startAdvert(bluetoothLeAdvertiser: BluetoothLeAdvertiser, callback: Callback<Triple<Boolean, AdvertiseCallback?, BluetoothGattServer?>>) {
        logger.debug("startAdvert")
        operationQueue.execute(Runnable {
            var result = true
            // var bluetoothGattServer: BluetoothGattServer? = null
            stopExistingGattServer()
            try {
                bluetoothGattServer = startGattServer(logger, context, payloadDataSupplier, database)
            } catch (e: Throwable) {
                logger.fault("startAdvert failed to start GATT server", e)
                result = false
            }
            if (bluetoothGattServer == null) {
                result = false
            } else {
                try {
                    setGattService(logger, context, bluetoothGattServer)
                } catch (e: Throwable) {
                    if (null != bluetoothGattServer) {
                        logger.fault("startAdvert failed to set GATT service", e)
                        bluetoothGattServer = try {
                            bluetoothGattServer!!.clearServices()
                            bluetoothGattServer!!.close()
                            null
                        } catch (e2: Throwable) {
                            logger.fault("startAdvert failed to stop GATT server", e2)
                            null
                        }
                    }
                    result = false
                }
            }
            if (!result) {
                logger.fault("startAdvert failed")
                callback.accept(Triple(false, null, null))
                return@Runnable
            }
            try {
                val bluetoothGattServerConfirmed = bluetoothGattServer
                val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
                    override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                        logger.debug("startAdvert successful")
                        callback.accept(Triple(true, this, bluetoothGattServerConfirmed))
                    }

                    override fun onStartFailure(errorCode: Int) {
                        logger.fault("startAdvert failed (errorCode={})", onStartFailureErrorCodeToString(errorCode))
                        callback.accept(Triple(false, this, bluetoothGattServerConfirmed))
                    }
                }
                startAdvertising(bluetoothLeAdvertiser, advertiseCallback)
            } catch (e: Throwable) {
                logger.fault("startAdvert failed")
                callback.accept(Triple(false, null, null))
            }
        })
    }

    private fun stopAdvert(bluetoothLeAdvertiser: BluetoothLeAdvertiser, advertiseCallback: AdvertiseCallback?, bluetoothGattServer: BluetoothGattServer?, callback: Callback<Boolean>) {
        logger.debug("stopAdvert")
        operationQueue.execute {
            var result = true
            try {
                if (advertiseCallback != null) {
                    bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
                }
            } catch (e: Throwable) {
                logger.fault("stopAdvert failed to stop advertising", e)
                result = false
            }
            try {
                if (bluetoothGattServer != null) {
                    bluetoothGattServer.clearServices()
                    bluetoothGattServer.close()
                }
            } catch (e: Throwable) {
                logger.fault("stopAdvert failed to stop GATT server", e)
                result = false
            }
            if (result) {
                logger.debug("stopAdvert successful")
            } else {
                logger.fault("stopAdvert failed")
            }
            callback.accept(result)
        }
    }

    override fun payloadData(): PayloadData {
        return payloadDataSupplier.payload(PayloadTimestamp(Date()))
    }

    override fun isSupported(): Boolean {
        return bluetoothLeAdvertiser() != null
    }

    override fun bluetoothStateManager(didUpdateState: BluetoothState) {
        logger.debug("didUpdateState (state={})", didUpdateState)
        if (didUpdateState == BluetoothState.poweredOn) {
            start()
        } else if (didUpdateState == BluetoothState.poweredOff) {
            stop()
        }
    }

    private fun startAdvertising(bluetoothLeAdvertiser: BluetoothLeAdvertiser, advertiseCallback: AdvertiseCallback) {
        logger.debug("startAdvertising")
        val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .build()
        val pseudoDeviceAddress = PseudoDeviceAddress()
        val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(BLESensorConfiguration.serviceUUID))
                .addManufacturerData(BLESensorConfiguration.manufacturerIdForSensor, pseudoDeviceAddress.data)
                .build()
        bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback)
        logger.debug("startAdvertising successful (pDeviceAddress={},settings={})", pseudoDeviceAddress, settings)
    }

    companion object {
        private val TAG = "ConcreteBLETransmitter"
        val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
        var bluetoothGattServer: BluetoothGattServer? = null

        private val advertOffDurationMillis = TimeInterval.seconds(4).millis()
        private fun startGattServer(logger: SensorLogger, context: Context, payloadDataSupplier: PayloadDataSupplier, database: BLEDatabase): BluetoothGattServer? {
            logger.debug("startGattServer")
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (bluetoothManager == null) {
                logger.fault("Bluetooth unsupported")
                return null
            }
            // Data = rssi (4 bytes int) + payload (remaining bytes)
            val server = AtomicReference<BluetoothGattServer?>(null)
            val callback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {

                //this should be a table
                //in order to handle many connections from different mac addresses
                //val writeDataPayload: MutableMap<String, ByteArray> = HashMap()
                //val readPayloadMap: MutableMap<String, ByteArray> = HashMap()

                ////in order to handle many connections from different mac addresses
                //Implement as Same as GattServer in Covid here
                private val onCharacteristicReadPayloadData: MutableMap<String, PayloadData?> = ConcurrentHashMap()
                private val onCharacteristicWriteSignalData: MutableMap<String, ByteArray> = ConcurrentHashMap()
                private fun onCharacteristicReadPayloadData(device: BluetoothDevice): PayloadData? {
                    logger.debug("startGattServer")
                    //Come here if othe phone is older version
                    val key = device.address
                    if (onCharacteristicReadPayloadData.containsKey(key)) {
                        return onCharacteristicReadPayloadData[key]
                    }
                    val payloadData = payloadDataSupplier.payload(PayloadTimestamp())
                    onCharacteristicReadPayloadData[key] = payloadData
                    return payloadData
                }

                private fun onCharacteristicWriteSignalData(device: BluetoothDevice, value: ByteArray?): ByteArray {
                    logger.debug("startGattServer")
                    val key = device.address
                    var partialData = onCharacteristicWriteSignalData[key]
                    if (partialData == null) {
                        partialData = ByteArray(0)
                    }
                    val data = ByteArray(partialData.size + (value?.size ?: 0))
                    System.arraycopy(partialData, 0, data, 0, partialData.size)
                    if (value != null) {
                        System.arraycopy(value, 0, data, partialData.size, value.size)
                    }
                    onCharacteristicWriteSignalData[key] = data
                    return data
                }

                private fun removeData(device: BluetoothDevice) {
                    val deviceAddress = device.address
                    for (deviceRequestId in ArrayList(onCharacteristicReadPayloadData.keys)) {
                        if (deviceRequestId.startsWith(deviceAddress)) {
                            onCharacteristicReadPayloadData.remove(deviceRequestId)
                        }
                    }
                    for (deviceRequestId in ArrayList(onCharacteristicWriteSignalData.keys)) {
                        if (deviceRequestId.startsWith(deviceAddress)) {
                            onCharacteristicWriteSignalData.remove(deviceRequestId)
                        }
                    }
                }

                override fun onConnectionStateChange(bluetoothDevice: BluetoothDevice?, status: Int, newState: Int) {
                    val device = database.device(bluetoothDevice)
                    logger.debug("onConnectionStateChange (device={},status={},newState={})",
                            device, status, onConnectionStateChangeStatusToString(newState))
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        //Save data here
                        device.state(BLEDeviceState.connected)
                        if (bluetoothDevice != null) {
                            bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).contains(bluetoothDevice)
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        device.state(BLEDeviceState.disconnected)
                        bluetoothDevice?.let { removeData(bluetoothDevice) }
                    }
                }

                // TODO We receive payload here
                override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
                    device?.let {
                        val targetDevice = database.device(device)
                        val targetIdentifier = targetDevice.identifier
                        logger.debug("didReceiveWrite (central={},requestId={},offset={},characteristic={},value={})",
                                targetDevice, requestId, offset,
                                if (characteristic.uuid == BLESensorConfiguration.androidSignalCharacteristicUUID) "signal" else "unknown",
                                value?.size ?: "null"
                        )
                        val data = Data(onCharacteristicWriteSignalData(device, value))
                        if (characteristic.uuid == BLESensorConfiguration.legacyCovidsafePayloadCharacteristicUUID) {
//                            val payloadData = SignalCharacteristicData.decodeWritePayload(data)
//                                    ?: // Fragmented payload data may be incomplete
//                                    return
                                    val payloadData = SignalCharacteristicData.decodeWritePayload(data)
                                    logger.debug("didReceiveWrite (dataType=payload,central={},payload={})", targetDevice, payloadData)
                            // Only receive-only Android devices write payload
//                            targetDevice.operatingSystem(BLEDeviceOperatingSystem.android)
//                            targetDevice.receiveOnly(true)
//                            targetDevice.payloadData(payloadData)
//                            onCharacteristicWriteSignalData.remove(device.address)
                            if (responseNeeded) {
                                server.get()?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                            }
                            return
                        }
                        if (characteristic.uuid !== BLESensorConfiguration.androidSignalCharacteristicUUID) {
                            if (responseNeeded) {
                                server.get()?.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, offset, value)
                            }
                            return
                        }
                        when (SignalCharacteristicData.detect(data)) {
                            SignalCharacteristicDataType.rssi -> {
                                val rssi = SignalCharacteristicData.decodeWriteRSSI(data)
                                if (rssi == null) {
                                    logger.fault("didReceiveWrite, invalid request (central={},action=writeRSSI)", targetDevice)
                                    return
                                }
                                logger.debug("didReceiveWrite (dataType=rssi,central={},rssi={})", targetDevice, rssi)
                                // Only receive-only Android devices write RSSI
                                targetDevice.operatingSystem(BLEDeviceOperatingSystem.android)
                                targetDevice.receiveOnly(true)
                                targetDevice.rssi(rssi)
                                return
                            }
                            SignalCharacteristicDataType.payload -> {
                                val payloadData = SignalCharacteristicData.decodeWritePayload(data)
                                        ?: // Fragmented payload data may be incomplete
                                        return
                                logger.debug("didReceiveWrite (dataType=payload,central={},payload={})", targetDevice, payloadData)
                                // Only receive-only Android devices write payload
                                targetDevice.operatingSystem(BLEDeviceOperatingSystem.android)
                                targetDevice.receiveOnly(true)
                                targetDevice.payloadData(payloadData)
                                onCharacteristicWriteSignalData.remove(device.address)
                                if (responseNeeded) {
                                    server.get()?.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, offset, value)
                                }
                                return
                            }
                            SignalCharacteristicDataType.payloadSharing -> {
                                val payloadSharingData = SignalCharacteristicData.decodeWritePayloadSharing(data)
                                        ?: // Fragmented payload sharing data may be incomplete
                                        return
                                val didSharePayloadData = payloadDataSupplier.payload(payloadSharingData.data)
                                for (delegate in BLETransmitter.delegates) {
                                    delegate.sensor(SensorType.BLE, didSharePayloadData, targetIdentifier)
                                }
                                // Only Android devices write payload sharing
                                targetDevice.operatingSystem(BLEDeviceOperatingSystem.android)
                                targetDevice.rssi(payloadSharingData.rssi)
                                logger.debug("didReceiveWrite (dataType=payloadSharing,central={},payloadSharingData={})", targetDevice, didSharePayloadData)
                                for (payloadData in didSharePayloadData) {
                                    val sharedDevice = database.device(payloadData)
                                    sharedDevice.operatingSystem(BLEDeviceOperatingSystem.shared)
                                    sharedDevice.rssi(payloadSharingData.rssi)
                                }
                                return
                            }
                        }
                        if (responseNeeded) {
                            server.get()?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                        }
                    }
                }

                inner class ReadRequestEncryptedPayload(val timestamp: Long, val modelP: String, val msg: String?)

                override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
                    device?.let {
                        //Come here if other phone is older version
                        val targetDevice = database.device(device)
                        if (characteristic?.uuid === BLESensorConfiguration.payloadCharacteristicUUID || characteristic?.uuid === BLESensorConfiguration.legacyCovidsafePayloadCharacteristicUUID) {
                            val payloadData = onCharacteristicReadPayloadData(device)
                            payloadData?.let {
                                if (offset > payloadData.value.size) {
                                    logger.fault("didReceiveRead, invalid offset (central={},requestId={},offset={},characteristic=payload,dataLength={})", targetDevice, requestId, offset, payloadData.value.size)
                                    server.get()?.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null)
                                } else {
                                    val value = Arrays.copyOfRange(payloadData.value, offset, payloadData.value.size)
                                    server.get()?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                                    logger.debug("didReceiveRead (central={},requestId={},offset={},characteristic=payload)", targetDevice, requestId, offset)
                                }
                            }
                        }
//                        else if (characteristic?.uuid === BLESensorConfiguration.serviceUUID) {
//                            //write here
//                            val peripheral = TracerApp.asPeripheralDevice()
//                            val readRequest = ReadRequestEncryptedPayload(
//                                    System.currentTimeMillis() / 1000L,
//                                    peripheral.modelP,
//                                    TracerApp.thisDeviceMsg()
//                            )
//                            val plainRecord = gson.toJson(readRequest)
//                            CentralLog.d(TAG, "onCharacteristicReadRequest plainRecord =  $plainRecord")
//
//                            val plainRecordByteArray = plainRecord.toByteArray(Charsets.UTF_8)
//                            val remoteBlob = Encryption.encryptPayload(plainRecordByteArray)
//                            val base = readPayloadMap.getOrPut(device.address, {
//                                ReadRequestPayload(
//                                        v = TracerApp.protocolVersion,
//                                        msg = remoteBlob,
//                                        org = TracerApp.ORG,
//                                        modelP = null //This is going to be stored as empty in the db as DUMMY value
//                                ).getPayload()
//                            })
//                            val value = base.copyOfRange(offset, base.size)
//                            server.get()?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
//
//                        }
                        else {
                            logger.fault("didReceiveRead (central={},characteristic=unknown)", targetDevice)
                            server.get()?.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null)
                        }
                    }
                }
            }
            server.set(bluetoothManager.openGattServer(context, callback))
            logger.debug("startGattServer successful")
            return server.get()
        }

        //Here
        private fun setGattService(logger: SensorLogger, context: Context, bluetoothGattServer: BluetoothGattServer?) {
            logger.debug("setGattService")
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (bluetoothManager == null) {
                logger.fault("Bluetooth unsupported")
                return
            }
            if (bluetoothGattServer == null) {
                logger.fault("Bluetooth LE advertiser unsupported")
                return
            }
            for (device in bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
                bluetoothGattServer.cancelConnection(device)
            }
            for (device in bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)) {
                bluetoothGattServer.cancelConnection(device)
            }
            bluetoothGattServer.clearServices()
            // Logic check - ensure there are now no Gatt Services
            var services = bluetoothGattServer.services
            for (svc in services) {
                logger.fault("setGattService device clearServices() call did not correctly clear service (service={})", svc.uuid)
            }

            val service = BluetoothGattService(BLESensorConfiguration.serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val signalCharacteristic = BluetoothGattCharacteristic(
                    BLESensorConfiguration.androidSignalCharacteristicUUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE)
            signalCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            val payloadCharacteristic = BluetoothGattCharacteristic(
                    BLESensorConfiguration.payloadCharacteristicUUID,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ)
            val legacyPayloadCharacteristicUUID = BluetoothGattCharacteristic(
                    BLESensorConfiguration.legacyCovidsafePayloadCharacteristicUUID,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ)
            service.addCharacteristic(signalCharacteristic)
            service.addCharacteristic(payloadCharacteristic)
            service.addCharacteristic(legacyPayloadCharacteristicUUID)
            bluetoothGattServer.addService(service)

            // Logic check - ensure there can be only one Herald service
            services = bluetoothGattServer.services
            var count = 0
            for (svc in services) {
                if (svc.uuid == BLESensorConfiguration.serviceUUID) {
                    count++
                }
            }
            if (count > 1) {
                logger.fault("setGattService device incorrectly sharing multiple Herald services (count={})", count)
            }

            logger.debug("setGattService successful (service={},signalCharacteristic={},payloadCharacteristic={})",
                    service.uuid, signalCharacteristic.uuid, payloadCharacteristic.uuid)
        }

        private fun onConnectionStateChangeStatusToString(state: Int): String {


            return when (state) {
                BluetoothProfile.STATE_CONNECTED -> "STATE_CONNECTED"
                BluetoothProfile.STATE_CONNECTING -> "STATE_CONNECTING"
                BluetoothProfile.STATE_DISCONNECTING -> "STATE_DISCONNECTING"
                BluetoothProfile.STATE_DISCONNECTED -> "STATE_DISCONNECTED"
                else -> "UNKNOWN_STATE_$state"
            }
        }

        private fun onStartFailureErrorCodeToString(errorCode: Int): String {
            return when (errorCode) {
                AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "ADVERTISE_FAILED_DATA_TOO_LARGE"
                AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
                AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "ADVERTISE_FAILED_ALREADY_STARTED"
                AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "ADVERTISE_FAILED_INTERNAL_ERROR"
                AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
                else -> "UNKNOWN_ERROR_CODE_$errorCode"
            }
        }
    }

    /**
     * Transmitter starts automatically when Bluetooth is enabled.
     */
    init {
        BluetoothStateManager.delegates.add(this)
        bluetoothStateManager(bluetoothStateManager.state())
        timer.add(AdvertLoopTask())
    }
}