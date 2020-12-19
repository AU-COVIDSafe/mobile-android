//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Unsigned integer (8 bits)
public class UInt8 {
    public final int value;
    public final Data bigEndian;

    public UInt8(int value) {
        assert(value >= 0);
        this.value = value;
        final ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.put((byte) value);
        this.bigEndian = new Data(byteBuffer.array());
    }
}
