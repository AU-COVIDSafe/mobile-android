//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

public class PayloadSharingData {
    public final RSSI rssi;
    public final Data data;

    /**
     * Payload sharing data
     *
     * @param rssi RSSI between self and peer.
     * @param data Payload data of devices being shared by self to peer.
     */
    public PayloadSharingData(final RSSI rssi, final Data data) {
        this.rssi = rssi;
        this.data = data;
    }
}
