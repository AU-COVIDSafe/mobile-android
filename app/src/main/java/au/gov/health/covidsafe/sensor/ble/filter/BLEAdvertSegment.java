//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble.filter;

import au.gov.health.covidsafe.sensor.datatype.Data;

public class BLEAdvertSegment {
    public final BLEAdvertSegmentType type;
    public final int dataLength;
    public final byte[] data; // BIG ENDIAN (network order) AT THIS POINT
    public final Data raw;

    public BLEAdvertSegment(BLEAdvertSegmentType type, int dataLength, byte[] data, Data raw) {
        this.type = type;
        this.dataLength = dataLength;
        this.data = data;
        this.raw = raw;
    }

    @Override
    public String toString() {
        return "BLEAdvertSegment{" +
                "type=" + type +
                ", dataLength=" + dataLength +
                ", data=" + new Data(data).hexEncodedString() +
                ", raw=" + raw.hexEncodedString() +
                '}';
    }
}
