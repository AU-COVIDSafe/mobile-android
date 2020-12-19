//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble;

import au.gov.health.covidsafe.BuildConfig;
import au.gov.health.covidsafe.sensor.data.SensorLoggerLevel;
import au.gov.health.covidsafe.sensor.datatype.TimeInterval;

import java.util.UUID;

/// Defines BLE sensor configuration data, e.g. service and characteristic UUIDs
public class BLESensorConfiguration {
    public final static SensorLoggerLevel logLevel = SensorLoggerLevel.debug;
    /**
     * Service UUID for beacon service. This is a fixed UUID to enable iOS devices to find each other even
     * in background mode. Android devices will need to find Apple devices first using the manufacturer code
     * then discover services to identify actual beacons.
     */
    public final static UUID serviceUUID = UUID.fromString(BuildConfig.BLE_SSID);
    /// Signaling characteristic for controlling connection between peripheral and central, e.g. keep each other from suspend state
    public final static UUID androidSignalCharacteristicUUID = UUID.fromString(BuildConfig.BLE_ANDROIDSIGNALCHARACTERISTIC);
    /// Signaling characteristic for controlling connection between peripheral and central, e.g. keep each other from suspend state
    public final static UUID iosSignalCharacteristicUUID = UUID.fromString(BuildConfig.BLE_IOSSIGNALCHARACTERISTIC);
    /// Primary payload characteristic (read) for distributing payload data from peripheral to central, e.g. identity data
    public final static UUID payloadCharacteristicUUID = UUID.fromString(BuildConfig.BLE_PAYLOADCHARACTERISTIC);
    public final static UUID legacyCovidsafePayloadCharacteristicUUID = UUID.fromString(BuildConfig.BLE_SSID);
    /// Expiry time for shared payloads, to ensure only recently seen payloads are shared, Sharing disabled for now as location permisssion on ios will allow scanning to work
    public static TimeInterval payloadSharingExpiryTimeInterval = TimeInterval.zero;
    /// Manufacturer data is being used on Android to store pseudo device address
    public final static int manufacturerIdForSensor = 65530;
    /// Advert refresh time interval
    public final static TimeInterval advertRefreshTimeInterval = TimeInterval.minutes(15);

    /// Signal characteristic action code for write payload, expect 1 byte action code followed by 2 byte little-endian Int16 integer value for payload data length, then payload data
    public final static byte signalCharacteristicActionWritePayload = (byte) 1;
    /// Signal characteristic action code for write RSSI, expect 1 byte action code followed by 4 byte little-endian Int32 integer value for RSSI value
    public final static byte signalCharacteristicActionWriteRSSI = (byte) 2;
    /// Signal characteristic action code for write payload, expect 1 byte action code followed by 2 byte little-endian Int16 integer value for payload sharing data length, then payload sharing data
    public final static byte signalCharacteristicActionWritePayloadSharing = (byte) 3;

    // BLE advert manufacturer ID for Apple, for scanning of background iOS devices
    public final static int manufacturerIdForApple = 76;

    /// Filter duplicate payload data and suppress sensor(didRead:fromTarget) delegate calls
    public static TimeInterval filterDuplicatePayloadData = TimeInterval.minutes(30);

    /// Define device filtering rules based on message patterns
    /// - Avoids connections to devices that cannot host sensor services
    /// - Matches against every manufacturer specific data message (Hex format) in advert
    /// - Java regular expression patterns, case insensitive, find pattern anywhere in message
    /// - Remember to include ^ to match from start of message
    /// - Use deviceFilterTrainingEnabled in development environment to identify patterns
    public static String[] deviceFilterFeaturePatterns = new String[]{
            "^10....04",
            "^10....14",
            "^0100000000000000000000000000000000",
            "^05","^07","^09",
            "^00",
            "^08","^03","^06",
            "^0C","^0D","^0F","^0E","^0B"
    };
}
