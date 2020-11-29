//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import au.gov.health.covidsafe.sensor.data.ConcreteSensorLogger;
import au.gov.health.covidsafe.sensor.data.SensorLogger;
import au.gov.health.covidsafe.sensor.datatype.Data;
import au.gov.health.covidsafe.sensor.datatype.PayloadData;
import au.gov.health.covidsafe.sensor.datatype.PayloadSharingData;
import au.gov.health.covidsafe.sensor.datatype.PseudoDeviceAddress;
import au.gov.health.covidsafe.sensor.datatype.RSSI;
import au.gov.health.covidsafe.sensor.datatype.TargetIdentifier;
import au.gov.health.covidsafe.streetpass.persistence.Encryption;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcreteBLEDatabase implements BLEDatabase, BLEDeviceDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLEDatabase");
    private final Queue<BLEDatabaseDelegate> delegates = new ConcurrentLinkedQueue<>();
    private final Map<TargetIdentifier, BLEDevice> database = new ConcurrentHashMap<>();
    private final ExecutorService queue = Executors.newSingleThreadExecutor();
    private static final String LOG_TAG = ConcreteBLEDatabase.class.getSimpleName();

    @Override
    public void add(BLEDatabaseDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public BLEDevice device(ScanResult scanResult) {
        final BluetoothDevice bluetoothDevice = scanResult.getDevice();
        // Get pseudo device address
        final PseudoDeviceAddress pseudoDeviceAddress = pseudoDeviceAddress(scanResult);
        if (pseudoDeviceAddress == null) {
            // Get device based on peripheral only
            return device(bluetoothDevice);
        }
        // Identify all existing devices with the same pseudo device address
        final List<BLEDevice> candidates = new ArrayList<>();
        for (final BLEDevice device : database.values()) {
            if (device.pseudoDeviceAddress() == null) {
                continue;
            }
            if (device.pseudoDeviceAddress().equals(pseudoDeviceAddress)) {
                candidates.add(device);
            }
        }
        // No existing device matching pseudo device address, create new device
        if (candidates.size() == 0) {
            final BLEDevice device = device(bluetoothDevice);
            device.pseudoDeviceAddress(pseudoDeviceAddress);
            return device;
        }
        // Find device with the same target identifier
        final TargetIdentifier targetIdentifier = new TargetIdentifier(bluetoothDevice);
        final BLEDevice existingDevice = database.get(targetIdentifier);
        if (existingDevice != null) {
            existingDevice.pseudoDeviceAddress(pseudoDeviceAddress);
            shareDataAcrossDevices(pseudoDeviceAddress);
            return existingDevice;
        }
        // Get most recent version of the device and clone to enable attachment to new peripheral
        Collections.sort(candidates, new Comparator<BLEDevice>() {
            @Override
            public int compare(BLEDevice d0, BLEDevice d1) {
                return Long.compare(d1.lastUpdatedAt.getTime(), d0.lastUpdatedAt.getTime());
            }
        });
        final BLEDevice cloneSource = candidates.get(0);
        final BLEDevice newDevice = new BLEDevice(cloneSource, scanResult.getDevice());
        database.put(newDevice.identifier, newDevice);
        queue.execute(new Runnable() {
            @Override
            public void run() {
                logger.debug("create (device={},pseudoAddress={})", newDevice.identifier, pseudoDeviceAddress);
                for (BLEDatabaseDelegate delegate : delegates) {
                    delegate.bleDatabaseDidCreate(newDevice);
                }
            }
        });
        newDevice.peripheral(scanResult.getDevice());
        final PayloadData payloadData = shareDataAcrossDevices(pseudoDeviceAddress);
        if (payloadData != null) {
            newDevice.payloadData(payloadData);
        }
        return newDevice;
    }

    /// Get pseudo device address for Android devices
    private PseudoDeviceAddress pseudoDeviceAddress(final ScanResult scanResult) {
        Log.d(LOG_TAG, "PseudoDeviceAddress");
        final ScanRecord scanRecord = scanResult.getScanRecord();
        if (scanRecord == null) {
            return null;
        }
        final byte[] data = scanRecord.getManufacturerSpecificData(BLESensorConfiguration.manufacturerIdForSensor);
//        if (data == null || data.length != 6) {
//            return null;
//        }
        if (data == null) {
            return null;
        }
        return new PseudoDeviceAddress(data);
    }

    /// Share information across devices with the same pseudo device address
    private PayloadData shareDataAcrossDevices(final PseudoDeviceAddress pseudoDeviceAddress) {
        // Get all devices with the same pseudo device address
        final List<BLEDevice> devices = new ArrayList<>();
        for (final BLEDevice device : database.values()) {
            if (device.pseudoDeviceAddress() == null) {
                continue;
            }
            if (device.pseudoDeviceAddress().equals(pseudoDeviceAddress)) {
                devices.add(device);
            }
        }
        // Get most recent version of payload data
        Collections.sort(devices, new Comparator<BLEDevice>() {
            @Override
            public int compare(BLEDevice d0, BLEDevice d1) {
                return Long.compare(d1.lastUpdatedAt.getTime(), d0.lastUpdatedAt.getTime());
            }
        });
        PayloadData payloadData = null;
        for (BLEDevice device : devices) {
            if (device.payloadData() != null) {
                payloadData = device.payloadData();
                break;
            }
        }
        // Distribute payload to all devices with the same pseudo address
        if (payloadData != null) {
            // Share it amongst devices within advert refresh time limit
            final long timeLimit = new Date().getTime() - BLESensorConfiguration.advertRefreshTimeInterval.millis();
            for (BLEDevice device : devices) {
                if (device.payloadData() == null && device.createdAt.getTime() >= timeLimit) {
                    device.payloadData(payloadData);
                }
            }
        }
        // Get the most complete operating system
        BLEDeviceOperatingSystem operatingSystem = null;
        for (BLEDevice device : devices) {
            if (device.operatingSystem() == BLEDeviceOperatingSystem.android || device.operatingSystem() == BLEDeviceOperatingSystem.ios) {
                operatingSystem = device.operatingSystem();
                break;
            }
        }
        // Distribute operating system to all devices with the same pseudo address
        if (operatingSystem != null) {
            for (BLEDevice device : devices) {
                if (device.operatingSystem() == BLEDeviceOperatingSystem.unknown
                        || device.operatingSystem() == BLEDeviceOperatingSystem.android_tbc
                        || device.operatingSystem() == BLEDeviceOperatingSystem.ios_tbc) {
                    device.operatingSystem(operatingSystem);
                }
            }
        }
        return payloadData;
    }


    @Override
    public BLEDevice device(BluetoothDevice bluetoothDevice) {
        final TargetIdentifier identifier = new TargetIdentifier(bluetoothDevice);
        BLEDevice device = database.get(identifier);
        if (device == null) {
            final BLEDevice newDevice = new BLEDevice(identifier, this);
            device = newDevice;
            database.put(identifier, newDevice);
            queue.execute(new Runnable() {
                @Override
                public void run() {
                    logger.debug("create (device={})", identifier);
                    for (BLEDatabaseDelegate delegate : delegates) {
                        delegate.bleDatabaseDidCreate(newDevice);
                    }
                }
            });
        }
        device.peripheral(bluetoothDevice);
        return device;
    }

    @Override
    public BLEDevice device(PayloadData payloadData) {
        BLEDevice device = null;
        for (BLEDevice candidate : database.values()) {
            if (payloadData.equals(candidate.payloadData())) {
                device = candidate;
                break;
            }
        }
        if (device == null) {
            final TargetIdentifier identifier = new TargetIdentifier();
            final BLEDevice newDevice = new BLEDevice(identifier, this);
            device = newDevice;
            database.put(identifier, newDevice);
            queue.execute(new Runnable() {
                @Override
                public void run() {
                    logger.debug("create (device={})", identifier);
                    for (BLEDatabaseDelegate delegate : delegates) {
                        delegate.bleDatabaseDidCreate(newDevice);
                    }
                }
            });
        }
        device.payloadData(payloadData);
        return device;
    }

    @Override
    public List<BLEDevice> devices() {
        return new ArrayList<>(database.values());
    }

    @Override
    public void delete(final TargetIdentifier identifier) {
        final BLEDevice device = database.remove(identifier);
        if (device != null) {
            queue.execute(new Runnable() {
                @Override
                public void run() {
                    logger.debug("delete (device={})", identifier);
                    for (final BLEDatabaseDelegate delegate : delegates) {
                        delegate.bleDatabaseDidDelete(device);
                    }
                }
            });
        }
    }

    @Override
    public PayloadSharingData payloadSharingData(final BLEDevice peer) {
        final RSSI rssi = peer.rssi();
        if (rssi == null) {
            return new PayloadSharingData(new RSSI(127), new Data(new byte[0]));
        }
        // Get other devices that were seen recently by this device
        final List<BLEDevice> unknownDevices = new ArrayList<>();
        final List<BLEDevice> knownDevices = new ArrayList<>();
        for (BLEDevice device : database.values()) {
            // Device was seen recently
            if (device.timeIntervalSinceLastUpdate().value >= BLESensorConfiguration.payloadSharingExpiryTimeInterval.value) {
                continue;
            }
            // Device has payload
            if (device.payloadData() == null) {
                continue;
            }
            // Device is iOS or receive only (Samsung J6)
            if (!(device.operatingSystem() == BLEDeviceOperatingSystem.ios || device.receiveOnly())) {
                continue;
            }
            // Payload is not the peer itself
            if (peer.payloadData() != null && (Arrays.equals(device.payloadData().value, peer.payloadData().value))) {
                continue;
            }
            // Payload is new to peer
            if (peer.payloadSharingData.contains(device.payloadData())) {
                knownDevices.add(device);
            } else {
                unknownDevices.add(device);
            }
        }
        // Most recently seen unknown devices first
        final List<BLEDevice> devices = new ArrayList<>();
        Collections.sort(unknownDevices, new Comparator<BLEDevice>() {
            @Override
            public int compare(BLEDevice d0, BLEDevice d1) {
                return Long.compare(d1.lastUpdatedAt.getTime(), d0.lastUpdatedAt.getTime());
            }
        });
        Collections.sort(knownDevices, new Comparator<BLEDevice>() {
            @Override
            public int compare(BLEDevice d0, BLEDevice d1) {
                return Long.compare(d1.lastUpdatedAt.getTime(), d0.lastUpdatedAt.getTime());
            }
        });
        devices.addAll(unknownDevices);
        if (devices.size() == 0) {
            return new PayloadSharingData(new RSSI(127), new Data(new byte[0]));
        }
        // Limit how much to share to avoid oversized data transfers over BLE
        // (512 bytes limit according to spec, 510 with response, iOS requires response)
        final Set<PayloadData> sharedPayloads = new HashSet<>(devices.size());
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (BLEDevice device : devices) {
            final PayloadData payloadData = device.payloadData();
            if (payloadData == null) {
                continue;
            }
            // Eliminate duplicates (this happens when the same device has changed address but the old version has not expired yet)
            if (sharedPayloads.contains(payloadData)) {
                continue;
            }
            // Limit payload sharing by BLE transfer limit
            if (payloadData.value.length + byteArrayOutputStream.toByteArray().length > 510) {
                break;
            }
            try {
                byteArrayOutputStream.write(payloadData.value);
                peer.payloadSharingData.add(payloadData);
                sharedPayloads.add(payloadData);
            } catch (Throwable e) {
                logger.fault("Failed to append payload sharing data", e);
            }
        }
        final Data data = new Data(byteArrayOutputStream.toByteArray());
        return new PayloadSharingData(rssi, data);
    }

    // MARK:- BLEDeviceDelegate

    @Override
    public void device(final BLEDevice device, final BLEDeviceAttribute didUpdate) {
        queue.execute(new Runnable() {
            @Override
            public void run() {
                logger.debug("update (device={},attribute={})", device.identifier, didUpdate.name());
                for (BLEDatabaseDelegate delegate : delegates) {
                    delegate.bleDatabaseDidUpdate(device, didUpdate);
                }
            }
        });
    }
}
