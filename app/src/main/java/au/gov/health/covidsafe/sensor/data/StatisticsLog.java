//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.data;

import android.content.Context;

import au.gov.health.covidsafe.sensor.DefaultSensorDelegate;
import au.gov.health.covidsafe.sensor.datatype.PayloadData;
import au.gov.health.covidsafe.sensor.datatype.Proximity;
import au.gov.health.covidsafe.sensor.datatype.Sample;
import au.gov.health.covidsafe.sensor.datatype.SensorType;
import au.gov.health.covidsafe.sensor.datatype.TargetIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// CSV contact log for post event analysis and visualisation
public class StatisticsLog extends DefaultSensorDelegate {
    private final TextFile textFile;
    private final PayloadData payloadData;
    private final Map<TargetIdentifier, String> identifierToPayload = new ConcurrentHashMap<>();
    private final Map<String, Date> payloadToTime = new ConcurrentHashMap<>();
    private final Map<String, Sample> payloadToSample = new ConcurrentHashMap<>();

    public StatisticsLog(final Context context, final String filename, final PayloadData payloadData) {
        textFile = new TextFile(context, filename);
        this.payloadData = payloadData;
    }

    private String csv(String value) {
        return TextFile.csv(value);
    }

    private void add(TargetIdentifier identifier) {
        final String payload = identifierToPayload.get(identifier);
        if (payload == null) {
            return;
        }
        add(payload);
    }

    private void add(String payload) {
        final Date time = payloadToTime.get(payload);
        final Sample sample = payloadToSample.get(payload);
        if (time == null || sample == null) {
            payloadToTime.put(payload, new Date());
            payloadToSample.put(payload, new Sample());
            return;
        }
        final Date now = new Date();
        payloadToTime.put(payload, now);
        sample.add((now.getTime() - time.getTime()) / 1000d);
        write();
    }

    private void write() {
        final StringBuilder content = new StringBuilder("payload,count,mean,sd,min,max\n");
        final List<String> payloadList = new ArrayList<>();
        for (String payload : payloadToSample.keySet()) {
            if (payload.equals(payloadData.shortName())) {
                continue;
            }
            payloadList.add(payload);
        }
        Collections.sort(payloadList);
        for (String payload : payloadList) {
            final Sample sample = payloadToSample.get(payload);
            if (sample == null) {
                continue;
            }
            if (sample.mean() == null || sample.standardDeviation() == null || sample.min() == null || sample.max() == null) {
                continue;
            }
            content.append(csv(payload));
            content.append(',');
            content.append(sample.count());
            content.append(',');
            content.append(sample.mean());
            content.append(',');
            content.append(sample.standardDeviation());
            content.append(',');
            content.append(sample.min());
            content.append(',');
            content.append(sample.max());
            content.append('\n');
        }
        textFile.overwrite(content.toString());
    }


    // MARK:- SensorDelegate

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
        identifierToPayload.put(fromTarget, didRead.shortName());
        add(fromTarget);
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
        add(fromTarget);
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        for (PayloadData payload : didShare) {
            add(payload.shortName());
        }
    }
}
