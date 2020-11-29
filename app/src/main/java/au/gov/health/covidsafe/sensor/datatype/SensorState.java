//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

/// Sensor state
public enum SensorState {
    /// Sensor is powered on, active and operational
    on,
    /// Sensor is powered off, inactive and not operational
    off,
    /// Sensor is not available
    unavailable
}
