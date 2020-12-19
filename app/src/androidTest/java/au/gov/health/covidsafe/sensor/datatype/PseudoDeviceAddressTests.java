//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package au.gov.health.covidsafe.sensor.datatype;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PseudoDeviceAddressTests {

    @Test
    public void testSecureRandom() {
        // Address should be different every time
        long last = 0;
        for (int i=0; i<1000; i++) {
            final SecureRandom secureRandom = PseudoDeviceAddress.getSecureRandom();
            final long value = secureRandom.nextLong();
            assertNotEquals(last, value);
            last = value;
        }
    }

    @Test
    public void testEncodeDecode() {
        // Test encoding and decoding to ensure same data means same address
        for (int i=0; i<1000; i++) {
            final PseudoDeviceAddress expected = new PseudoDeviceAddress();
            final PseudoDeviceAddress actual = new PseudoDeviceAddress(expected.data);
            assertEquals(expected.address, actual.address);
        }
    }

    @Test
    public void testRandomBytes() {
        // Every byte should rotate (most of the time)
        byte[] last = new byte[6];
        for (int i=0; i<10; i++) {
            final PseudoDeviceAddress address = new PseudoDeviceAddress();
            assertEquals(6, address.data.length);
            for (int j=0; j<6; j++) {
                assertNotEquals(address.data[j], last[j]);
            }
            last = address.data;
        }
    }

    @Test
    public void testVisualCheck() {
        // Visual check for randomness and byte fill
        for (int i=0; i<10; i++) {
            final PseudoDeviceAddress address = new PseudoDeviceAddress();
            System.err.println(Arrays.toString(address.data));
        }
    }
}