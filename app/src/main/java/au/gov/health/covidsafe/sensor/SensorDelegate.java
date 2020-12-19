//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor;

import au.gov.health.covidsafe.sensor.ble.BLEDevice;
import au.gov.health.covidsafe.sensor.datatype.Location;
import au.gov.health.covidsafe.sensor.datatype.PayloadData;
import au.gov.health.covidsafe.sensor.datatype.Proximity;
import au.gov.health.covidsafe.sensor.datatype.SensorState;
import au.gov.health.covidsafe.sensor.datatype.SensorType;
import au.gov.health.covidsafe.sensor.datatype.TargetIdentifier;

import java.util.List;

/// Sensor delegate for receiving sensor events.
public interface SensorDelegate {
    /// Detection of a target with an ephemeral identifier, e.g. BLE central detecting a BLE peripheral.
    void sensor(SensorType sensor, TargetIdentifier didDetect);

    /// Read payload data from target, e.g. encrypted device identifier from BLE peripheral after successful connection.
    void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget);

    /// Read payload data of other targets recently acquired by a target, e.g. Android peripheral sharing payload data acquired from nearby iOS peripherals.
    void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget);

    /// Measure proximity to target, e.g. a sample of RSSI values from BLE peripheral.
    void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget);

    /// Detection of time spent at location, e.g. at specific restaurant between 02/06/2020 19:00 and 02/06/2020 21:00
    void sensor(SensorType sensor, Location didVisit);

    /// Measure proximity to target with payload data. Combines didMeasure and didRead into a single convenient delegate method
    void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget, PayloadData withPayload, BLEDevice device);

    void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget, PayloadData withPayload);

    /// Sensor state update
    void sensor(SensorType sensor, SensorState didUpdateState);

    /// Measure proximity to target with payload data. Combines didMeasure and didRead into a single convenient delegate method
    void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget, Proximity atProximity, int withTxPower, BLEDevice device);
}
