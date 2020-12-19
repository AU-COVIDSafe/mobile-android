//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor;

/// Sensor for detecting and tracking various kinds of disease transmission vectors, e.g. contact with people, time at location.
public interface Sensor {
    /// Add delegate for responding to sensor events.
    void add(SensorDelegate delegate);

    /// Start sensing.
    void start();

    /// Stop sensing.
    void stop();
}
