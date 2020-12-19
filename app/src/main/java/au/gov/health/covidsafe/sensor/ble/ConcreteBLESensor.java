//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble;

import android.content.Context;
import android.util.Log;

import com.atlassian.mobilekit.module.feedback.commands.SendFeedbackCommand;

import au.gov.health.covidsafe.sensor.SensorDelegate;
import au.gov.health.covidsafe.sensor.data.ConcreteSensorLogger;
import au.gov.health.covidsafe.sensor.data.SensorLogger;
import au.gov.health.covidsafe.sensor.datatype.BluetoothState;
import au.gov.health.covidsafe.sensor.datatype.PayloadData;
import au.gov.health.covidsafe.sensor.datatype.Proximity;
import au.gov.health.covidsafe.sensor.datatype.ProximityMeasurementUnit;
import au.gov.health.covidsafe.sensor.datatype.RSSI;
import au.gov.health.covidsafe.sensor.datatype.SensorState;
import au.gov.health.covidsafe.sensor.datatype.SensorType;
import au.gov.health.covidsafe.sensor.datatype.TimeInterval;
import au.gov.health.covidsafe.sensor.payload.PayloadDataSupplier;

import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcreteBLESensor implements BLESensor, BLEDatabaseDelegate, BluetoothStateManagerDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLESensor");
    private final Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();
    private final BLETransmitter transmitter;
    private final BLEReceiver receiver;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();
    private static final String LOG_TAG = ConcreteBLESensor.class.getSimpleName();

    // Record payload data to enable de-duplication
    private final Map<PayloadData, Date> didReadPayloadData = new ConcurrentHashMap<>();

    public ConcreteBLESensor(Context context, PayloadDataSupplier payloadDataSupplier) {
        final BluetoothStateManager bluetoothStateManager = new ConcreteBluetoothStateManager(context);
        final BLEDatabase database = new ConcreteBLEDatabase();
        final BLETimer timer = new BLETimer(context);
        bluetoothStateManager.delegates.add(this);
        transmitter = new ConcreteBLETransmitter(context, bluetoothStateManager, timer, payloadDataSupplier, database);
        receiver = new ConcreteBLEReceiver(context, bluetoothStateManager, timer, database, transmitter);
        database.add(this);
    }

    @Override
    public void add(SensorDelegate delegate) {
        delegates.add(delegate);
        transmitter.add(delegate);
        receiver.add(delegate);
    }

    @Override
    public void start() {
        logger.debug("start");
        // BLE transmitter and receivers start on powerOn event
        transmitter.start();
        receiver.start();
    }

    @Override
    public void stop() {
        logger.debug("stop");
        // BLE transmitter and receivers stops on powerOff event
        transmitter.stop();
        receiver.stop();
    }

    // MARK:- BLEDatabaseDelegate

    @Override
    public void bleDatabaseDidCreate(final BLEDevice device) {
        logger.debug("didDetect (device={},payloadData={})", device.identifier, device.payloadData());
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                for (SensorDelegate delegate : delegates) {
                    delegate.sensor(SensorType.BLE, device.identifier);
                }
            }
        });
    }

    @Override
    public void bleDatabaseDidUpdate(final BLEDevice device, BLEDeviceAttribute attribute) {
        //Save in database here
        Log.d(LOG_TAG, "attribute:" + attribute);
        Log.d(LOG_TAG, "device:" + device);
        switch (attribute) {
            case rssi: {
                final RSSI rssi = device.rssi();
                if (rssi == null) {
                    return;
                }
                final Proximity proximity = new Proximity(ProximityMeasurementUnit.RSSI, (double) rssi.value);
                Log.d(LOG_TAG, "device.payloadData():" + device.payloadData());
                Log.d(LOG_TAG, "device:" + device);
                Log.d(LOG_TAG, "proximity.description():" + proximity.description());
                // We receive payload here
                logger.debug("didMeasure (device={},payloadData={},proximity={})", device, device.payloadData(), proximity.description());
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, proximity, device.identifier);
                        }
                    }
                });
                final PayloadData payloadData = device.payloadData();
                if (payloadData == null) {
                    return;
                }
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, proximity, device.identifier, payloadData,device);
                        }
                    }
                });
                break;
            }
            case payloadData: {
                final PayloadData payloadData = device.payloadData();
                if (payloadData == null) {
                    return;
                }
                // De-duplicate payload in recent time
                if (BLESensorConfiguration.filterDuplicatePayloadData != TimeInterval.never) {
                    final long removePayloadDataBefore = new Date().getTime() - BLESensorConfiguration.filterDuplicatePayloadData.millis();
                    for (Map.Entry<PayloadData, Date> entry : didReadPayloadData.entrySet()) {
                        if (entry.getValue().getTime() < removePayloadDataBefore) {
                            didReadPayloadData.remove(entry.getKey());
                        }
                    }
                    final Date lastReportedAt = didReadPayloadData.get(payloadData);
                    if (lastReportedAt != null) {
                        logger.debug("didRead, filtered duplicate (device={},payloadData={},lastReportedAt={})", device, device.payloadData().shortName(), lastReportedAt);
                        return;
                    }
                    didReadPayloadData.put(payloadData, new Date());
                }
                // Notify delegates
                logger.debug("didRead (device={},payloadData={},payloadData={})", device, device.payloadData(), payloadData.shortName());
                final RSSI rssi = device.rssi();
                final Proximity proximity = new Proximity(ProximityMeasurementUnit.RSSI, (double) rssi.value);
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (SensorDelegate delegate : delegates) {
                            if (device.txPower() != null) {
                                delegate.sensor(SensorType.BLE, payloadData, device.identifier, proximity, device.txPower().value, device);
                            } else {
                                delegate.sensor(SensorType.BLE, payloadData, device.identifier, proximity, 999, device);
                            }
                        }
                    }
                });
                break;
            }
            default: {
            }
        }
    }

    @Override
    public void bleDatabaseDidDelete(BLEDevice device) {
        logger.debug("didDelete (device={})", device.identifier);
    }

    // MARK:- BluetoothStateManagerDelegate

    @Override
    public void bluetoothStateManager(BluetoothState didUpdateState) {
        logger.debug("didUpdateState (state={})", didUpdateState);
        SensorState sensorState = SensorState.off;
        if (didUpdateState == BluetoothState.poweredOn) {
            sensorState = SensorState.on;
        } else if (didUpdateState == BluetoothState.unsupported) {
            sensorState = SensorState.unavailable;
        }
        for (SensorDelegate delegate : delegates) {
            delegate.sensor(SensorType.BLE, sensorState);
        }
    }
}
