//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

/// Free text place name.
public class PlacenameLocationReference implements LocationReference {
    public final String name;

    public PlacenameLocationReference(String name) {
        this.name = name;
    }

    public String description() {
        return "PLACE(name=" + name + ")";
    }
}
