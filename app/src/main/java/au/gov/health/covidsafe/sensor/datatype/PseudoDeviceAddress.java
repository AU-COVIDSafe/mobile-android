//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

import android.util.Base64;

import au.gov.health.covidsafe.sensor.data.ConcreteSensorLogger;
import au.gov.health.covidsafe.sensor.data.SensorLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;

/// Pseudo device address to enable caching of device payload without relying on device mac address
// that may change frequently like the A10 and A20.
public class PseudoDeviceAddress {
    public final long address;
    public final byte[] data;

    /// Using secure random can cause blocking on app initialisation due to lack of entropy
    /// on some devices. Worst case scenario is app blocking upon initialisation, bluetooth
    /// power cycle, or advert refresh that occurs once every 15 minutes, leading to zero
    /// detection until sufficient entropy has been collected, which may take time given
    /// the device is likely to be idle. Not using secure random is acceptable and recommended
    /// in this instance because it is non-blocking and the sequence has sufficient uncertainty
    /// introduced programmatically to make an attack impractical from limited obeservations.
    public PseudoDeviceAddress() {
        // Bluetooth device address is 48-bit (6 bytes), using
        // the same length to offer the same collision avoidance
        // Choose between random, secure random, and NIST compliant secure random as random source
        // - Random is non-blocking and sufficiently secure for this purpose, recommended
        // - SecureRandom is potentially blocking and unnecessary in this instance, not recommended
        // - NISTSecureRandom is most likely to block and unnecessary in this instance, not recommended
        this.data = encode(getSecureRandomLong());
        this.address = decode(this.data);
    }

    public PseudoDeviceAddress(final byte[] data) {
        this.data = data;
        this.address = decode(data);
    }

    /// Non-blocking random number generator with appropriate strength for this purpose
    protected final static long getRandomLong() {
        // Use a different instance with random seed from another sequence each time
        final Random random = new Random(Math.round(Math.random() * Long.MAX_VALUE));
        // Skip a random number of bytes from another sequence
        random.nextBytes(new byte[256 + (int) Math.round(Math.random() * 1024)]);
        return random.nextLong();
    }

    /// Secure random number generator that is potentially blocking. Experiments have
    /// shown blocking can occur, especially on idle device, due to lack of entropy.
    protected final static long getSecureRandomLong() {
        return new SecureRandom().nextLong();
    }

    private static SecureRandom secureRandomSingleton = null;
    /// Secure random number generator that is potentially blocking.
    protected final static long getSecureRandomSingletonLong() {
        // On-demand initialisation in the hope that sufficient
        // entropy has been gathered during app initialisation
        if (secureRandomSingleton == null) {
            secureRandomSingleton = new SecureRandom();
        }
        return secureRandomSingleton.nextLong();
    }

    /// Get secure random instance seed according to NIST SP800-90A recommendations
    /// - SHA1PRNG algorithm
    /// - Algorithm seeded with 440 bits of secure random data
    /// - Skips first random number of bytes to mitigate against poor implementations
    /// Compliance to NIST SP800-90A offers quality assurance against an accepted
    /// standard. The aim here is not to offer the most perfect random source, but
    /// a source with well defined and understood characteristics, thus enabling
    /// selection of the most appropropriate method, given the intented purpose.
    /// This implementation supports security strength for NIST SP800-57
    /// Part 1 Revision 5 (informally, generation of cryptographic keys for
    /// encryption of sensitive data).
    public final static long getNISTSecureRandomLong() {
        try {
            // Obtain SHA1PRNG specifically where possible for NIST SP800-90A compliance.
            // Ignoring Android recommendation to use "new SecureRandom()" because that
            // decision was taken based on a single peer reviewed statistical test that
            // showed SHA1PRNG has bias. The test has not been adopted by NIST yet which
            // already uses 15 other statistical tests for quality assurance. This does
            // not mean the new test is invalid, but it is more appropriate for this work
            // to adopt and comply with an accepted standard for security assurance.
            final SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            // Obtain the most secure PRNG claimed by the platform for generating the seed
            // according to Android recommendation.
            final SecureRandom secureRandomForSeed = new SecureRandom();
            // NIST SP800-90A (see section 10.1) recommends 440 bit seed for SHA1PRNG
            // to support security strength defined in NIST SP800-57 Part 1 Revision 5.
            final byte[] seed = secureRandomForSeed.generateSeed(55);
            // Seed secure random with 440 bit seed according to NIST SP800-90A recommendation.
            secureRandom.setSeed(seed); // seed with random number
            // Skip the first 256 - 1280 bytes as mitigation against poor implementations
            // of SecureRandom where the initial values are predictable given the seed
            secureRandom.nextBytes(new byte[256 + secureRandom.nextInt(1024)]);
            return secureRandom.nextLong();
        } catch (Throwable e) {
            // Android OS may mandate the use of "new SecureRandom()" and forbid the use
            // of a specific provider in the future. Fallback to Android mandated option
            // and log the fact that it is no longer NIST SP800-90A compliant.
            final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Datatype.PseudoDeviceAddress");
            logger.fault("NIST SP800-90A compliant SecureRandom initialisation failed, reverting back to SecureRandom", e);
            return getSecureRandomLong();
        }
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