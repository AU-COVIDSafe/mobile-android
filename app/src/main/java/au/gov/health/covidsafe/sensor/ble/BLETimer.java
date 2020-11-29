//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble;

import android.content.Context;
import android.os.PowerManager;

import au.gov.health.covidsafe.sensor.data.ConcreteSensorLogger;
import au.gov.health.covidsafe.sensor.data.SensorLogger;
import au.gov.health.covidsafe.sensor.datatype.Sample;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Steady one second timer for controlling BLE operations. Having a reliable timer for starting
 * and stopping scans is fundamental for reliable detection and tracking. Methods that have been
 * tested and failed included :
 * 1. Handler.postDelayed loop backed by MainLooper
 * - Actual delay time can drift to 10+ minutes for a 4 second request.
 * 2. Handler.postDelayed loop backed by dedicated looper backed by dedicated HandlerThread
 * - Slightly better than option (1) but actual delay time can still drift to several minutes for a 4 second request.
 * 3. Timer scheduled task loop
 * - Timer can drift
 * <p>
 * Test impact of power management by ...
 * <p>
 * 1. Run app on device
 * 2. Keep one device connected via USB only
 * 3. Put app in background mode and lock device
 * 4. Go to terminal
 * 5. cd  ~/Library/Android/sdk/platform-tools
 * <p>
 * Test DOZE mode
 * 1. ./adb shell dumpsys battery unplug
 * 2. Expect "powerSource=battery" on log
 * 3. ./adb shell dumpsys deviceidle force-idle
 * 4. Expect "idle=true" on log
 * <p>
 * Exit DOZE mode
 * 1. ./adb shell dumpsys deviceidle unforce
 * 2. ./adb shell dumpsys battery reset
 * 3. Expect "idle=false" and "powerSource=usb/ac" on log
 * <p>
 * Test APP STANDBY mode
 * 1. ./adb shell dumpsys battery unplug
 * 2. Expect "powerSource=battery"
 * 3. ./adb shell am set-inactive au.gov.health.covidsafe true
 * <p>
 * Exit APP STANDBY mode
 * 1. ./adb shell am set-inactive au.gov.health.covidsafe false
 * 2. ./adb shell am get-inactive au.gov.health.covidsafe
 * 3. Expect "idle=false" on terminal
 * 4. ./adb shell dumpsys battery reset
 * 5. Expect "powerSource=usb/ac" on log
 */
public class BLETimer {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLETimer");
    private final Sample sample = new Sample();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final PowerManager.WakeLock wakeLock;
    private final AtomicLong now = new AtomicLong(0);
    private final Queue<BLETimerDelegate> delegates = new ConcurrentLinkedQueue<>();
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            for (BLETimerDelegate delegate : delegates) {
                try {
                    delegate.bleTimer(now.get());
                } catch (Throwable e) {
                    logger.fault("delegate execution failed", e);
                }
            }
        }
    };

    public BLETimer(Context context) {
        final PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sensor:BLETimer");
        wakeLock.acquire();
        final Thread timerThread = new Thread(new Runnable() {
            private long last = 0;

            @Override
            public void run() {
                while (true) {
                    now.set(System.currentTimeMillis());
                    final long elapsed = now.get() - last;
                    if (elapsed >= 1000) {
                        if (last != 0) {
                            sample.add(elapsed);
                            executorService.execute(runnable);
                        }
                        last = now.get();
                    }
                    try {
                        Thread.sleep(500);
                    } catch (Throwable e) {
                        logger.fault("Timer interrupted", e);
                    }
                }
            }
        });
        timerThread.setPriority(Thread.MAX_PRIORITY);
        timerThread.setName("Sensor.BLETimer");
        timerThread.start();
    }

    @Override
    protected void finalize() {
        wakeLock.release();
    }

    /// Add delegate for time notification
    public void add(BLETimerDelegate delegate) {
        delegates.add(delegate);
    }
}
