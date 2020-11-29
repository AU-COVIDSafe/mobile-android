//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

import androidx.annotation.NonNull;

/// Raw data for estimating proximity between sensor and target, e.g. RSSI for BLE.
public class Proximity {
    /// Unit of measurement, e.g. RSSI
    public final ProximityMeasurementUnit unit;
    /// Measured value, e.g. raw RSSI value.
    public final Double value;

    public Proximity(ProximityMeasurementUnit unit, Double value) {
        this.unit = unit;
        this.value = value;
    }

    /// Get plain text description of proximity data
    public String description() {
        return unit + ":" + value;
    }

    @NonNull
    @Override
    public String toString() {
        return description();
    }
}
