//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

/// Encrypted payload data received from target. This is likely to be an encrypted datagram of the target's actual permanent identifier.
public class PayloadData extends Data {
    public PayloadData(byte[] value) {
        super(value);
    }

    public PayloadData(String base64EncodedString) {
        super(base64EncodedString);
    }

    public PayloadData(byte repeating, int count) {
        super(repeating, count);
    }

    public PayloadData() {
        this(new byte[0]);
    }

    public String shortName() {
        if (value.length == 0) {
            return "";
        }
        if (!(value.length > 3)) {
            return Base64.encode(value);
        }
        final Data subdata = subdata(3, value.length - 3);
        final byte[] suffix = (subdata == null || subdata.value == null ? new byte[0] : subdata.value);
        final String base64EncodedString = Base64.encode(suffix);
        return base64EncodedString.substring(0, Math.min(6, base64EncodedString.length()));
    }

    public String toString() {
        return shortName();
    }
}
