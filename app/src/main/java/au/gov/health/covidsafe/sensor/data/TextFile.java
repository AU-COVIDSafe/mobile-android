//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.data;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TextFile {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.TextFile");
    private final File file;

    public TextFile(final Context context, final String filename) {
        final File folder = new File(getRootFolder(context), "Sensor");
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                logger.fault("Make folder failed (folder={})", folder);
            }
        }
        file = new File(folder, filename);
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Get root folder for SD card or emulated external storage.
     *
     * @param context Application context.
     * @return Root folder.
     */
    private static File getRootFolder(final Context context) {
        // Get SD card or emulated external storage. By convention (really!?)
        // SD card is reported after emulated storage, so select the last folder
        final File[] externalMediaDirs = context.getExternalMediaDirs();
        if (externalMediaDirs.length > 0) {
            return externalMediaDirs[externalMediaDirs.length - 1];
        } else {
            return Environment.getExternalStorageDirectory();
        }
    }

    public synchronized boolean empty() {
        return !file.exists() || file.length() == 0;
    }

    /// Append line to new or existing file
    public synchronized void write(String line) {
        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write((line + "\n").getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Throwable e) {
            logger.fault("write failed (file={})", file, e);
        }
    }

    /// Overwrite file content
    public synchronized void overwrite(String content) {
        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(content.getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Throwable e) {
            logger.fault("overwrite failed (file={})", file, e);
        }
    }

    /// Quote value for CSV output if required.
    public static String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("'") || value.contains("’")) {
            return "\"" + value + "\"";
        } else {
            return value;
        }
    }
}
