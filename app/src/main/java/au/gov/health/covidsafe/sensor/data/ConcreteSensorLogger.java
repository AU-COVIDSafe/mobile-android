//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.data;

import android.content.Context;
import android.util.Log;

import au.gov.health.covidsafe.BuildConfig;
import au.gov.health.covidsafe.sensor.ble.BLESensorConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;

/// Concrete Sensor log is for debug purposes. This will be removed in the production build.
public class ConcreteSensorLogger implements SensorLogger {
    private final String subsystem, category;
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static Context context;
    private static TextFile logFile;

    public ConcreteSensorLogger(String subsystem, String category) {
        this.subsystem = subsystem;
        this.category = category;
    }

    public static void context(final Context context) {
        if (context != null && context != ConcreteSensorLogger.context) {
            ConcreteSensorLogger.context = context;
            if (BuildConfig.DEBUG) {
                logFile = new TextFile(context, "log.txt");
            }
        }
    }

    private boolean suppress(SensorLoggerLevel level) {
        switch (level) {
            case debug:
                return (BLESensorConfiguration.logLevel == SensorLoggerLevel.info || BLESensorConfiguration.logLevel == SensorLoggerLevel.fault);
            case info:
                return (BLESensorConfiguration.logLevel == SensorLoggerLevel.fault);
            default:
                return false;
        }
    }

    private void log(SensorLoggerLevel level, String message, final Object... values) {
        if (!suppress(level)) {
            outputLog(level, tag(subsystem, category), message, values);
            outputStream(level, subsystem, category, message, values);
        }
    }

    public void debug(String message, final Object... values) {
        log(SensorLoggerLevel.debug, message, values);
    }

    public void info(String message, final Object... values) {
        log(SensorLoggerLevel.info, message, values);
    }

    public void fault(String message, final Object... values) {
        log(SensorLoggerLevel.fault, message, values);
    }

    private static String tag(String subsystem, String category) {
        return subsystem + "::" + category;
    }

    private static void outputLog(final SensorLoggerLevel level, final String tag, final String message, final Object... values) {
        final Throwable throwable = getThrowable(values);
        switch (level) {
            case debug: {
                if (throwable == null) {
                    Log.d(tag, render(message, values));
                } else {
                    Log.d(tag, render(message, values), throwable);
                }
                break;
            }
            case info: {
                if (throwable == null) {
                    Log.i(tag, render(message, values));
                } else {
                    Log.i(tag, render(message, values), throwable);
                }
                break;
            }
            case fault: {
                if (throwable == null) {
                    Log.w(tag, render(message, values));
                } else {
                    Log.w(tag, render(message, values), throwable);
                }
                break;
            }
        }
    }

    private static void outputStream(final SensorLoggerLevel level, final String subsystem, final String category, final String message, final Object... values) {
        if (logFile == null) {
            return;
        }
        final String timestamp = dateFormatter.format(new Date());
        final String csvMessage = render(message, values).replace('\"', '\'');
        final String quotedMessage = (message.contains(",") ? "\"" + csvMessage + "\"" : csvMessage);
        final String entry = timestamp + "," + level + "," + subsystem + "," + category + "," + quotedMessage;
        logFile.write(entry);
    }


    private static Throwable getThrowable(final Object... values) {
        if (values.length > 0 && values[values.length - 1] instanceof Throwable) {
            return (Throwable) values[values.length - 1];
        } else {
            return null;
        }
    }

    private static String render(final String message, final Object... values) {
        if (values.length == 0) {
            return message;
        } else {
            final StringBuilder stringBuilder = new StringBuilder();

            int valueIndex = 0;
            int start = 0;
            int end = message.indexOf("{}");
            while (end > 0) {
                stringBuilder.append(message.substring(start, end));
                if (values.length > valueIndex) {
                    if (values[valueIndex] == null) {
                        stringBuilder.append("NULL");
                    } else {
                        stringBuilder.append(values[valueIndex].toString());
                    }
                }
                valueIndex++;
                start = end + 2;
                end = message.indexOf("{}", start);
            }
            stringBuilder.append(message.substring(start));

            return stringBuilder.toString();
        }
    }

}
