//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import au.gov.health.covidsafe.BuildConfig;
import au.gov.health.covidsafe.sensor.ble.ConcreteBLESensor;
import au.gov.health.covidsafe.sensor.data.BatteryLog;
import au.gov.health.covidsafe.sensor.data.ConcreteSensorLogger;
import au.gov.health.covidsafe.sensor.data.ContactLog;
import au.gov.health.covidsafe.sensor.data.DetectionLog;
import au.gov.health.covidsafe.sensor.data.SensorLogger;
import au.gov.health.covidsafe.sensor.data.StatisticsLog;
import au.gov.health.covidsafe.sensor.datatype.PayloadData;
import au.gov.health.covidsafe.sensor.datatype.PayloadTimestamp;
import au.gov.health.covidsafe.sensor.payload.PayloadDataSupplier;

import java.util.ArrayList;
import java.util.List;

/// Sensor array for combining multiple detection and tracking methods.
public class SensorArray implements Sensor {
    private final Context context;
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "SensorArray");
    private final List<Sensor> sensorArray = new ArrayList<>();

    private final PayloadData payloadData;
    public final static String deviceDescription = android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")";


    public SensorArray(Context context, PayloadDataSupplier payloadDataSupplier) {
        this.context = context;
        // Ensure logger has been initialised (should have happened in AppDelegate already)
        ConcreteSensorLogger.context(context);
        logger.debug("init");

        // Start foreground service to enable background scan
//        final Intent intent = new Intent(context, ForegroundService.class);
        //final Intent intentBleService = new Intent(context, BluetoothMonitoringService.class);
        //context.startService(intentBleService);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context.startForegroundService(intent);
//        } else {
//            context.startService(intent);
//        }

        // Define sensor array
        sensorArray.add(new ConcreteBLESensor(context, payloadDataSupplier));

        // Loggers
        payloadData = payloadDataSupplier.payload(new PayloadTimestamp());
        if (BuildConfig.DEBUG) {
            add(new ContactLog(context, "contacts.csv"));
            add(new StatisticsLog(context, "statistics.csv", payloadData));
            add(new DetectionLog(context,"detection.csv", payloadData));
            new BatteryLog(context, "battery.csv");
        }
    //Get Device payload
        logger.info("DEVICE (payload={},description={})", payloadData.shortName(), deviceDescription);
    }

    public final PayloadData payloadData() {
        return payloadData;
    }

    @Override
    public void add(final SensorDelegate delegate) {
        for (Sensor sensor : sensorArray) {
            sensor.add(delegate);
        }
    }

    @Override
    public void start() {
        logger.debug("start");
        for (Sensor sensor : sensorArray) {
            sensor.start();
        }
    }

    @Override
    public void stop() {
        logger.debug("stop");
        for (Sensor sensor : sensorArray) {
            sensor.stop();
        }
    }
}
