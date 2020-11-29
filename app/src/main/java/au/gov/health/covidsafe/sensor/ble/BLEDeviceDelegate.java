//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble;

public interface BLEDeviceDelegate {
    void device(BLEDevice device, BLEDeviceAttribute didUpdate);
}

