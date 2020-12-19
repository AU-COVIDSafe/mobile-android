//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.data;

import android.content.Context;

import au.gov.health.covidsafe.sensor.DefaultSensorDelegate;
import au.gov.health.covidsafe.sensor.ble.BLEDevice;
import au.gov.health.covidsafe.sensor.datatype.PayloadData;
import au.gov.health.covidsafe.sensor.datatype.Proximity;
import au.gov.health.covidsafe.sensor.datatype.SensorType;
import au.gov.health.covidsafe.sensor.datatype.TargetIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// CSV contact log for post event analysis and visualisation
public class DetectionLog extends DefaultSensorDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.DetectionLog");
    private final TextFile textFile;
    private final PayloadData payloadData;
    private final String deviceName = android.os.Build.MODEL;
    private final String deviceOS = Integer.toString(android.os.Build.VERSION.SDK_INT);
    private final Map<String, String> payloads = new ConcurrentHashMap<>();

    public DetectionLog(final Context context, final String filename, final PayloadData payloadData) {
        textFile = new TextFile(context, filename);
        this.payloadData = payloadData;
        write();
    }

    private String csv(String value) {
        return TextFile.csv(value);
    }

    private void write() {
        final StringBuilder content = new StringBuilder();
        content.append(csv(deviceName));
        content.append(',');
        content.append("Android");
        content.append(',');
        content.append(csv(deviceOS));
        content.append(',');
        content.append(csv(payloadData.shortName()));
        final List<String> payloadList = new ArrayList<>(payloads.size());
        for (String payload : payloads.keySet()) {
            if (payload.equals(payloadData.shortName())) {
                continue;
            }
            payloadList.add(payload);
        }
        Collections.sort(payloadList);
        for (String payload : payloadList) {
            content.append(',');
            content.append(payload);
        }
        logger.debug("write (content={})", content.toString());
        content.append("\n");
        textFile.overwrite(content.toString());
    }


    // MARK:- SensorDelegate

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
        if (payloads.put(didRead.shortName(), fromTarget.value) == null) {
            logger.debug("didRead (payload={})", payloadData.shortName());
            write();
        }
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        for (PayloadData payloadData : didShare) {
            if (payloads.put(payloadData.shortName(), fromTarget.value) == null) {
                logger.debug("didShare (payload={})", payloadData.shortName());
                write();
            }
        }
    }
}
