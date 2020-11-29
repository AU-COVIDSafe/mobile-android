//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble;

import au.gov.health.covidsafe.sensor.datatype.BluetoothState;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface BluetoothStateManager {
    Queue<BluetoothStateManagerDelegate> delegates = new ConcurrentLinkedQueue<>();

    BluetoothState state();
}
