//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble.filter;

import au.gov.health.covidsafe.sensor.datatype.Data;

public class BLEAdvertAppleManufacturerSegment {
    public final int type;
    public final int reportedLength;
    public final byte[] data; // BIG ENDIAN (network order) AT THIS POINT
    public final Data raw;

    public BLEAdvertAppleManufacturerSegment(int type, int reportedLength, byte[] dataBigEndian, Data raw) {
        this.type = type;
        this.reportedLength = reportedLength;
        this.data = dataBigEndian;
        this.raw = raw;
    }

    @Override
    public String toString() {
        return raw.hexEncodedString();
    }
}
