//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/// Pseudo device address to enable caching of device payload without relying on device mac address
// that may change frequently like the A10 and A20.
public class PseudoDeviceAddress {
    public final long address;
    public final byte[] data;

    public PseudoDeviceAddress() {
        // Bluetooth device address is 48-bit (6 bytes), using
        // the same length to offer the same collision avoidance
        address = Math.round(Math.random() * Math.pow(2, 48));
        final ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putLong(0, address);
        // Only taking last 6 bytes as that is the maximum value
        data = new byte[6];
        System.arraycopy(byteBuffer.array(), 2, data, 0, data.length);
    }

    public PseudoDeviceAddress(final byte[] data) {
        this.data = data;
        final ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.position(2);
        byteBuffer.put(data);
        this.address = byteBuffer.getLong(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PseudoDeviceAddress that = (PseudoDeviceAddress) o;
        return address == that.address;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public String toString() {
        return Base64.encodeToString(data, Base64.DEFAULT | Base64.NO_WRAP);
    }
}
