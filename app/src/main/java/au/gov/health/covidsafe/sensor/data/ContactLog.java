//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.data;

import android.content.Context;

import au.gov.health.covidsafe.sensor.DefaultSensorDelegate;
import au.gov.health.covidsafe.sensor.datatype.Location;
import au.gov.health.covidsafe.sensor.datatype.PayloadData;
import au.gov.health.covidsafe.sensor.datatype.Proximity;
import au.gov.health.covidsafe.sensor.datatype.SensorType;
import au.gov.health.covidsafe.sensor.datatype.TargetIdentifier;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/// CSV contact log for post event analysis and visualisation
public class ContactLog extends DefaultSensorDelegate {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final TextFile textFile;

    public ContactLog(final Context context, final String filename) {
        textFile = new TextFile(context, filename);
        if (textFile.empty()) {
            textFile.write("time,sensor,id,detect,read,measure,share,visit,data");
        }
    }

    private String timestamp() {
        return dateFormatter.format(new Date());
    }

    private String csv(String value) {
        return TextFile.csv(value);
    }

    // MARK:- SensorDelegate

    @Override
    public void sensor(SensorType sensor, TargetIdentifier didDetect) {
        textFile.write(timestamp() + "," + sensor.name() + "," + csv(didDetect.value) + ",1,,,,,");
    }

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
        textFile.write(timestamp() + "," + sensor.name() + "," + csv(fromTarget.value) + ",,2,,,," + csv(didRead.shortName()));
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        final String prefix = timestamp() + "," + sensor.name() + "," + csv(fromTarget.value);
        for (PayloadData payloadData : didShare) {
            textFile.write(prefix + ",,,,4,," + csv(payloadData.shortName()));
        }
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
        textFile.write(timestamp() + "," + sensor.name() + "," + csv(fromTarget.value) + ",,,3,,," + csv(didMeasure.description()));
    }

    @Override
    public void sensor(SensorType sensor, Location didVisit) {
        textFile.write(timestamp() + "," + sensor.name() + ",,,,,,5," + csv(didVisit.description()));
    }
}
