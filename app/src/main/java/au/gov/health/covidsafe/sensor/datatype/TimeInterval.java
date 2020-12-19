//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

/// Time interval in seconds.
public class TimeInterval {
    public final long value;
    public static final TimeInterval minute = new TimeInterval(60);
    public static final TimeInterval zero = new TimeInterval(0);
    public static final TimeInterval never = new TimeInterval(Long.MAX_VALUE);

    public TimeInterval(long seconds) {
        this.value = seconds;
    }

    public static TimeInterval minutes(long minutes) {
        return new TimeInterval(minute.value * minutes);
    }

    public static TimeInterval seconds(long seconds) {
        return new TimeInterval(seconds);
    }

    public long millis() {
        return value * 1000;
    }

    @Override
    public String toString() {
        if (value == never.value) {
            return "never";
        }
        return Long.toString(value);
    }
}
