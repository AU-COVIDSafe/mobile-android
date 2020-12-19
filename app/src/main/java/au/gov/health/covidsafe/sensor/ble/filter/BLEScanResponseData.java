//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble.filter;

import java.util.List;

public class BLEScanResponseData {
    public int dataLength;
    public List<BLEAdvertSegment> segments;

    public BLEScanResponseData(int dataLength, List<BLEAdvertSegment> segments) {
        this.dataLength = dataLength;
        this.segments = segments;
    }

    @Override
    public String toString() {
        return segments.toString();
    }
}
