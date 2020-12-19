//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

/// Raw location data for estimating indirect exposure, e.g. WGS84 coordinates
public interface LocationReference {
    String description();
}
