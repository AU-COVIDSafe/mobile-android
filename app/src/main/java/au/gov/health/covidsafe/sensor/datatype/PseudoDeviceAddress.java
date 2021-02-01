//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

import android.util.Base64;
import au.gov.health.covidsafe.sensor.data.ConcreteSensorLogger;
import au.gov.health.covidsafe.sensor.data.SensorLogger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.security.SecureRandom;
import java.util.Random;

/// Pseudo device address to enable caching of device payload without relying on device mac address
// that may change frequently like the A10 and A20.
public class PseudoDeviceAddress {
    public final long address;
    public final byte[] data;

    public PseudoDeviceAddress() {
        // Bluetooth device address is 48-bit (6 bytes), using
        // the same length to offer the same collision avoidance
        this.data = encode(getSecureRandomSingletonLong());
        this.address = decode(this.data);
    }

    public PseudoDeviceAddress(final byte[] data) {
        this.data = data;
        this.address = decode(data);
    }

    protected final static long getSecureRandomLong() {
        return new SecureRandom().nextLong();
    }

    private static SecureRandom secureRandomSingleton = null;
    protected final static long getSecureRandomSingletonLong() {
        // On-demand initialisation in the hope that sufficient
        // entropy has been gathered during app initialisation
        if (secureRandomSingleton == null) {
            secureRandomSingleton = new SecureRandom();
        }
        return secureRandomSingleton.nextLong();
    }

    protected final static byte[] encode(final long value) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putLong(0, value);
        final byte[] data = new byte[6];
        System.arraycopy(byteBuffer.array(), 0, data, 0, data.length);
        return data;
    }

    protected final static long decode(final byte[] data) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.put(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getLong(0);
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