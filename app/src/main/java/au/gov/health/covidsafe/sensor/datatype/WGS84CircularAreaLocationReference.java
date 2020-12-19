//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

/// GPS coordinates and region radius, e.g. latitude and longitude in decimal format and radius in meters.
public class WGS84CircularAreaLocationReference implements LocationReference {
    public final Double latitude, longitude, altitude, radius;

    public WGS84CircularAreaLocationReference(Double latitude, Double longitude, Double altitude, Double radius) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.radius = radius;
    }

    public String description() {
        return "WGS84(lat=" + latitude + ",lon=" + longitude + ",alt=" + altitude + ",radius=" + radius + ")";
    }
}
