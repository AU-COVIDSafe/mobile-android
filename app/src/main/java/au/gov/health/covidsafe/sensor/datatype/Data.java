//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

import java.util.Arrays;

/// Raw byte array data
public class Data {
    private final static char[] hexChars = "0123456789ABCDEF".toCharArray();
    public byte[] value = null;

    public Data() {
        this(new byte[0]);
    }

    public Data(byte[] value) {
        this.value = value;
    }

    public Data(final Data data) {
        final byte[] value = new byte[data.value.length];
        System.arraycopy(data.value, 0, value, 0, data.value.length);
        this.value = value;
    }

    public Data(byte repeating, int count) {
        this.value = new byte[count];
        for (int i=count; i-->0;) {
            this.value[i] = repeating;
        }
    }

    public Data(String base64EncodedString) {
        this.value = Base64.decode(base64EncodedString);
    }

    public String base64EncodedString() {
        return Base64.encode(value);
    }

    public String hexEncodedString() {
        if (value == null) {
            return "";
        }
        final StringBuilder stringBuilder = new StringBuilder(value.length * 2);
        for (int i = 0; i < value.length; i++) {
            final int v = value[i] & 0xFF;
            stringBuilder.append(hexChars[v >>> 4]);
            stringBuilder.append(hexChars[v & 0x0F]);
        }
        return stringBuilder.toString();
    }

    public final static Data fromHexEncodedString(String hexEncodedString) {
        final int length = hexEncodedString.length();
        final byte[] value = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            value[i / 2] = (byte) ((Character.digit(hexEncodedString.charAt(i), 16) << 4) +
                    Character.digit(hexEncodedString.charAt(i+1), 16));
        }
        return new Data(value);
    }

    public String description() {
        return base64EncodedString();
    }

    /// Get subdata from offset to end
    public Data subdata(int offset) {
        if (offset < value.length) {
            final byte[] offsetValue = new byte[value.length - offset];
            System.arraycopy(value, offset, offsetValue, 0, offsetValue.length);
            return new Data(offsetValue);
        } else {
            return null;
        }
    }

    /// Get subdata from offset to offset + length
    public Data subdata(int offset, int length) {
        if (offset + length <= value.length) {
            final byte[] offsetValue = new byte[length];
            System.arraycopy(value, offset, offsetValue, 0, length);
            return new Data(offsetValue);
        } else {
            return null;
        }
    }

    /// Append data to end of this data.
    public void append(Data data) {
        final byte[] concatenated = new byte[value.length + data.value.length];
        System.arraycopy(value, 0, concatenated, 0, value.length);
        System.arraycopy(data.value, 0, concatenated, value.length, data.value.length);
        value = concatenated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Data data = (Data) o;
        return Arrays.equals(value, data.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return hexEncodedString();
    }
}
