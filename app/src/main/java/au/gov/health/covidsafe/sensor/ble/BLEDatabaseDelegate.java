//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble;

/// Delegate for receiving registry create/update/delete events
public interface BLEDatabaseDelegate {
    void bleDatabaseDidCreate(BLEDevice device);

    void bleDatabaseDidUpdate(BLEDevice device, BLEDeviceAttribute attribute);

    void bleDatabaseDidDelete(BLEDevice device);
}
