//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

/// GPS coordinates (latitude,longitude,altitude) in WGS84 decimal format and meters from sea level.
public class WGS84PointLocationReference implements LocationReference {
    public final Double latitude, longitude, altitude;

    public WGS84PointLocationReference(Double latitude, Double longitude, Double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public String description() {
        return "WGS84(lat=" + latitude + ",lon=" + longitude + ",alt=" + altitude + ")";
    }
}
