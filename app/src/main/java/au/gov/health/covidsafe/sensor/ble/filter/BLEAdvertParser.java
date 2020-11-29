//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble.filter;

import au.gov.health.covidsafe.sensor.datatype.Data;
import au.gov.health.covidsafe.sensor.datatype.UInt8;

import java.util.ArrayList;
import java.util.List;

public class BLEAdvertParser {
    public static BLEScanResponseData parseScanResponse(byte[] raw, int offset) {
        // Multiple segments until end of binary data
        return new BLEScanResponseData(raw.length - offset, extractSegments(raw, offset));
    }

    public static List<BLEAdvertSegment> extractSegments(byte[] raw, int offset) {
        int position = offset;
        ArrayList<BLEAdvertSegment> segments = new ArrayList<BLEAdvertSegment>();
        int segmentLength;
        int segmentType;
        byte[] segmentData;
        Data rawData;
        int c;

        while (position < raw.length) {
            if ((position + 2) <= raw.length) {
                segmentLength = (byte)raw[position++] & 0xff;
                segmentType = (byte)raw[position++] & 0xff;
                // Note: Unsupported types are handled as 'unknown'
                // check reported length with actual remaining data length
                if ((position + segmentLength - 1) <= raw.length) {
                    segmentData = subDataBigEndian(raw, position, segmentLength - 1); // Note: type IS INCLUDED in length
                    rawData = new Data(subDataBigEndian(raw, position - 2, segmentLength + 1));
                    position += segmentLength - 1;
                    segments.add(new BLEAdvertSegment(BLEAdvertSegmentType.typeFor(segmentType), segmentLength - 1, segmentData, rawData));
                } else {
                    // error in data length - advance to end
                    position = raw.length;
                }
            } else {
                // invalid segment - advance to end
                position = raw.length;
            }
        }

        return segments;
    }

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static String binaryString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            result.append(" ");
        }
        return result.toString();
    }

    public static byte[] subDataBigEndian(byte[] raw, int offset, int length) {
        if (raw == null) {
            return new byte[]{};
        }
        if (offset < 0 || length <= 0) {
            return new byte[]{};
        }
        if (length + offset > raw.length) {
            return new byte[]{};
        }
        byte[] data = new byte[length];
        int position = offset;
        for (int c = 0;c < length;c++) {
            data[c] = raw[position++];
        }
        return data;
    }

    public static byte[] subDataLittleEndian(byte[] raw, int offset, int length) {
        if (raw == null) {
            return new byte[]{};
        }
        if (offset < 0 || length <= 0) {
            return new byte[]{};
        }
        if (length + offset > raw.length) {
            return new byte[]{};
        }
        byte[] data = new byte[length];
        int position = offset + length - 1;
        for (int c = 0;c < length;c++) {
            data[c] = raw[position--];
        }
        return data;
    }

    public static Integer extractTxPower(List<BLEAdvertSegment> segments) {
        // find the txPower code segment in the list
        for (BLEAdvertSegment segment : segments) {
            if (segment.type == BLEAdvertSegmentType.txPowerLevel) {
                return (new UInt8((int)segment.data[0])).value;
            }
        }
        return null;
    }

    public static List<BLEAdvertManufacturerData> extractManufacturerData(List<BLEAdvertSegment> segments) {
        // find the manufacturerData code segment in the list
        List<BLEAdvertManufacturerData> manufacturerData = new ArrayList<>();
        for (BLEAdvertSegment segment : segments) {
            if (segment.type == BLEAdvertSegmentType.manufacturerData) {
                // Ensure that the data area is long enough
                if (segment.data.length < 2) {
                    continue; // there may be a valid segment of same type... Happens for manufacturer data
                }
                // Create a manufacturer data segment
                int intValue = ((segment.data[1]&0xff) << 8) | (segment.data[0]&0xff);
                manufacturerData.add(new BLEAdvertManufacturerData(intValue,subDataBigEndian(segment.data,2,segment.dataLength - 2), segment.raw));
            }
        }
        return manufacturerData;
    }

    public static List<BLEAdvertAppleManufacturerSegment> extractAppleManufacturerSegments(List<BLEAdvertManufacturerData> manuData) {
        final List<BLEAdvertAppleManufacturerSegment> appleSegments = new ArrayList<>();
        for (BLEAdvertManufacturerData manu : manuData) {
            int bytePos = 0;
            while (bytePos < manu.data.length) {
                final byte type = manu.data[bytePos];
                final int typeValue = type & 0xFF;
                // "01" marks legacy service UUID encoding without length data
                if (type == 0x01) {
                    final int length = manu.data.length - bytePos - 1;
                    final Data data = new Data(subDataBigEndian(manu.data, bytePos + 1, length));
                    final Data raw = new Data(subDataBigEndian(manu.data, bytePos, manu.data.length - bytePos));
                    final BLEAdvertAppleManufacturerSegment segment = new BLEAdvertAppleManufacturerSegment(typeValue, length, data.value, raw);
                    appleSegments.add(segment);
                    bytePos = manu.data.length;
                }
                // Parse according to Type-Length-Data
                else {
                    final int length = manu.data[bytePos + 1] & 0xFF;
                    final int maxLength = (length < manu.data.length - bytePos - 2 ? length : manu.data.length - bytePos - 2);
                    final Data data = new Data(subDataBigEndian(manu.data, bytePos + 2, maxLength));
                    final Data raw = new Data(subDataBigEndian(manu.data, bytePos, maxLength + 2));
                    final BLEAdvertAppleManufacturerSegment segment = new BLEAdvertAppleManufacturerSegment(typeValue, length, data.value, raw);
                    appleSegments.add(segment);
                    bytePos += (maxLength + 2);
                }
            }
        }
        return appleSegments;
    }
}
