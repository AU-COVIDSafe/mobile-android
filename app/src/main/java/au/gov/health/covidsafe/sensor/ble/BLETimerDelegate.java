//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble;

public interface BLETimerDelegate {

    void bleTimer(long currentTimeMillis);
}
