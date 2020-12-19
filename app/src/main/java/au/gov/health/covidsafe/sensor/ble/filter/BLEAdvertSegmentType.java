//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble.filter;

import java.util.HashMap;
import java.util.Map;

/// BLE Advert types - Note: We only list those we use in Herald for some reason
/// See https://www.bluetooth.com/specifications/assigned-numbers/generic-access-profile/
public enum BLEAdvertSegmentType {
    unknown("unknown", 0x00), // Valid - this number is not assigned
    serviceUUID16IncompleteList("serviceUUID16IncompleteList", 0x02),
    serviceUUID16CompleteList("serviceUUID16CompleteList", 0x03),
    serviceUUID32IncompleteList("serviceUUID32IncompleteList", 0x04),
    serviceUUID32CompleteList("serviceUUID32CompleteList", 0x05),
    serviceUUID128IncompleteList("serviceUUID128IncompleteList", 0x06),
    serviceUUID128CompleteList("serviceUUID128CompleteList", 0x07),
    deviceNameShortened("deviceNameShortened", 0x08),
    deviceNameComplete("deviceNameComplete", 0x09),
    txPowerLevel("txPower",0x0A),
    deviceClass("deviceClass",0x0D),
    simplePairingHash("simplePairingHash",0x0E),
    simplePairingRandomiser("simplePairingRandomiser",0x0F),
    deviceID("deviceID",0x10),
    meshMessage("meshMessage",0x2A),
    meshBeacon("meshBeacon",0x2B),
    bigInfo("bigInfo",0x2C),
    broadcastCode("broadcastCode",0x2D),
    manufacturerData("manufacturerData", 0xFF)
    ;

    private static final Map<String, BLEAdvertSegmentType> BY_LABEL = new HashMap<>();
    private static final Map<Integer, BLEAdvertSegmentType> BY_CODE = new HashMap<>();
    static {
        for (BLEAdvertSegmentType e : values()) {
            BY_LABEL.put(e.label, e);
            BY_CODE.put(e.code, e);
        }
    }

    public final String label;
    public final int code;

    private BLEAdvertSegmentType(String label, int code) {
        this.label = label;
        this.code = code;
    }

    public static BLEAdvertSegmentType typeFor(int code) {
        BLEAdvertSegmentType type = BY_CODE.get(code);
        if (null == type) {
            return BY_LABEL.get("unknown");
        }
        return type;
    }

    public static BLEAdvertSegmentType typeFor(String commonName) {
        BLEAdvertSegmentType type = BY_LABEL.get(commonName);
        if (null == type) {
            return BY_LABEL.get("unknown");
        }
        return type;
    }
}
