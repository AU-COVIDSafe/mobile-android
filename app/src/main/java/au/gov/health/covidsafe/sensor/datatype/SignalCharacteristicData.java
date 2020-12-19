//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

import au.gov.health.covidsafe.sensor.ble.BLESensorConfiguration;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Codec for signal characteristic data bundles
public class SignalCharacteristicData {

    /// Encode write RSSI data bundle
    // writeRSSI data format
    // 0-0 : actionCode
    // 1-2 : rssi value (Int16)
    public static Data encodeWriteRssi(final RSSI rssi) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(3);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(0, BLESensorConfiguration.signalCharacteristicActionWriteRSSI);
        byteBuffer.putShort(1, (short) rssi.value);
        return new Data(byteBuffer.array());
    }

    /// Decode write RSSI data bundle
    public static RSSI decodeWriteRSSI(final Data data) {
        if (signalDataActionCode(data.value) != BLESensorConfiguration.signalCharacteristicActionWriteRSSI) {
            return null;
        }
        if (data.value.length != 3) {
            return null;
        }
        final Short rssiValue = int16(data.value, 1);
        if (rssiValue == null) {
            return null;
        }
        return new RSSI(rssiValue.intValue());
    }

    /// Encode write payload data bundle
    // writePayload data format
    // 0-0 : actionCode
    // 1-2 : payload data count in bytes (Int16)
    // 3.. : payload data
    public static Data encodeWritePayload(final PayloadData payloadData) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(3 + payloadData.value.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(0, BLESensorConfiguration.signalCharacteristicActionWritePayload);
        byteBuffer.putShort(1, (short) payloadData.value.length);
        byteBuffer.position(3);
        byteBuffer.put(payloadData.value);
        return new Data(byteBuffer.array());
    }

    /// Decode write payload data bundle
    public static PayloadData decodeWritePayload(final Data data) {
        if (signalDataActionCode(data.value) != BLESensorConfiguration.signalCharacteristicActionWritePayload) {
            return null;
        }
        if (data.value.length < 3) {
            return null;
        }
        final Short payloadDataCount = int16(data.value, 1);
        if (payloadDataCount == null) {
            return null;
        }
        if (data.value.length != (3 + payloadDataCount.intValue())) {
            return null;
        }
        final Data payloadDataBytes = new Data(data.value).subdata(3);
        if (payloadDataBytes == null) {
            return null;
        }
        return new PayloadData(payloadDataBytes.value);
    }

    /// Encode write payload sharing data bundle
    // writePayloadSharing data format
    // 0-0 : actionCode
    // 1-2 : rssi value (Int16)
    // 3-4 : payload sharing data count in bytes (Int16)
    // 5.. : payload sharing data
    public static Data encodeWritePayloadSharing(final PayloadSharingData payloadSharingData) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(5 + payloadSharingData.data.value.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(0, BLESensorConfiguration.signalCharacteristicActionWritePayloadSharing);
        byteBuffer.putShort(1, (short) payloadSharingData.rssi.value);
        byteBuffer.putShort(3, (short) payloadSharingData.data.value.length);
        byteBuffer.position(5);
        byteBuffer.put(payloadSharingData.data.value);
        return new Data(byteBuffer.array());
    }

    /// Decode write payload data bundle
    public static PayloadSharingData decodeWritePayloadSharing(final Data data) {
        if (signalDataActionCode(data.value) != BLESensorConfiguration.signalCharacteristicActionWritePayloadSharing) {
            return null;
        }
        if (data.value.length < 5) {
            return null;
        }
        final Short rssiValue = int16(data.value, 1);
        if (rssiValue == null) {
            return null;
        }
        final Short payloadSharingDataCount = int16(data.value, 3);
        if (payloadSharingDataCount == null) {
            return null;
        }
        if (data.value.length != (5 + payloadSharingDataCount.intValue())) {
            return null;
        }
        final Data payloadSharingDataBytes = new Data(data.value).subdata(5);
        if (payloadSharingDataBytes == null) {
            return null;
        }
        return new PayloadSharingData(new RSSI(rssiValue.intValue()), payloadSharingDataBytes);
    }

    /// Detect signal characteristic data bundle type
    public static SignalCharacteristicDataType detect(Data data) {
        switch (signalDataActionCode(data.value)) {
            case BLESensorConfiguration.signalCharacteristicActionWriteRSSI:
                return SignalCharacteristicDataType.rssi;
            case BLESensorConfiguration.signalCharacteristicActionWritePayload:
                return SignalCharacteristicDataType.payload;
            case BLESensorConfiguration.signalCharacteristicActionWritePayloadSharing:
                return SignalCharacteristicDataType.payloadSharing;
            default:
                return SignalCharacteristicDataType.unknown;
        }
    }

    private static byte signalDataActionCode(byte[] signalData) {
        if (signalData == null || signalData.length == 0) {
            return 0;
        }
        return signalData[0];
    }

    private static Short int16(byte[] data, int index) {
        if (index < data.length - 1) {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            return byteBuffer.getShort(index);
        } else {
            return null;
        }
    }
}
