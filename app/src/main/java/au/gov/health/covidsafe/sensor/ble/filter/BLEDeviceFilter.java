//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble.filter;

import android.bluetooth.le.ScanRecord;

import au.gov.health.covidsafe.sensor.ble.BLEDevice;
import au.gov.health.covidsafe.sensor.ble.BLESensorConfiguration;
import au.gov.health.covidsafe.sensor.data.ConcreteSensorLogger;
import au.gov.health.covidsafe.sensor.data.SensorLogger;
import au.gov.health.covidsafe.sensor.datatype.Data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Device filter for avoiding connection to devices that definitely cannot
/// host sensor services.
public class BLEDeviceFilter {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEDeviceFilter");
    private final List<FilterPattern> filterPatterns;
    private final Map<Data, ShouldIgnore> samples = new HashMap<>();

    // Counter for training samples
    private final static class ShouldIgnore {
        public long yes = 0;
        public long no = 0;
    }

    // Pattern for filtering device based on message content
    public final static class FilterPattern {
        public final String regularExpression;
        public final Pattern pattern;
        public FilterPattern(final String regularExpression, final Pattern pattern) {
            this.regularExpression = regularExpression;
            this.pattern = pattern;
        }
    }

    // Match of a filter pattern
    public final static class MatchingPattern {
        public final FilterPattern filterPattern;
        public final String message;
        public MatchingPattern(FilterPattern filterPattern, String message) {
            this.filterPattern = filterPattern;
            this.message = message;
        }
    }

    /// BLE device filter for matching devices against filters defined
    /// in BLESensorConfiguration.deviceFilterFeaturePatterns.
    public BLEDeviceFilter() {
        this(BLESensorConfiguration.deviceFilterFeaturePatterns);
    }

    /// BLE device filter for matching devices against the given set of patterns
    /// and writing advert data to file for analysis.
    public BLEDeviceFilter(final String[] patterns) {
        if (patterns == null || patterns.length == 0) {
            filterPatterns = null;
        } else {
            filterPatterns = compilePatterns(patterns);
        }
    }

    // MARK:- Pattern matching functions
    // Using regular expression over hex representation of feature data for maximum flexibility and usability

    /// Match message against all patterns in sequential order, returns matching pattern or null
    protected static FilterPattern match(final List<FilterPattern> filterPatterns, final String message) {
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEDeviceFilter");
        if (message == null) {
            return null;
        }
        for (final FilterPattern filterPattern : filterPatterns) {
            try {
                final Matcher matcher = filterPattern.pattern.matcher(message);
                if (matcher.find()) {
                    return filterPattern;
                }
            } catch (Throwable e) {
            }
        }
        return null;
    }

    /// Compile regular expressions into patterns.
    protected static List<FilterPattern> compilePatterns(final String[] regularExpressions) {
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEDeviceFilter");
        final List<FilterPattern> filterPatterns = new ArrayList<>(regularExpressions.length);
        for (final String regularExpression : regularExpressions) {
            try {
                final Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);
                final FilterPattern filterPattern = new FilterPattern(regularExpression, pattern);
                filterPatterns.add(filterPattern);
            } catch (Throwable e) {
                logger.fault("compilePatterns, invalid filter pattern (regularExpression={})", regularExpression);
            }
        }
        return filterPatterns;
    }

    /// Extract messages from manufacturer specific data
    protected final static List<Data> extractMessages(final byte[] rawScanRecordData) {
        // Parse raw scan record data in scan response data
        if (rawScanRecordData == null || rawScanRecordData.length == 0) {
            return null;
        }
        final BLEScanResponseData bleScanResponseData = BLEAdvertParser.parseScanResponse(rawScanRecordData, 0);
        // Parse scan response data into manufacturer specific data
        if (bleScanResponseData == null || bleScanResponseData.segments == null || bleScanResponseData.segments.isEmpty()) {
            return null;
        }
        final List<BLEAdvertManufacturerData> bleAdvertManufacturerDataList = BLEAdvertParser.extractManufacturerData(bleScanResponseData.segments);
        // Parse manufacturer specific data into messages
        if (bleAdvertManufacturerDataList == null || bleAdvertManufacturerDataList.isEmpty()) {
            return null;
        }
        final List<BLEAdvertAppleManufacturerSegment> bleAdvertAppleManufacturerSegments = BLEAdvertParser.extractAppleManufacturerSegments(bleAdvertManufacturerDataList);
        // Convert segments to messages
        if (bleAdvertAppleManufacturerSegments == null || bleAdvertAppleManufacturerSegments.isEmpty()) {
            return null;
        }
        final List<Data> messages = new ArrayList<>(bleAdvertAppleManufacturerSegments.size());
        for (BLEAdvertAppleManufacturerSegment segment : bleAdvertAppleManufacturerSegments) {
            if (segment.raw != null && segment.raw.value.length > 0) {
                messages.add(segment.raw);
            }
        }
        return messages;
    }

    // MARK:- Filtering functions

    /// Extract feature data from scan record
    private List<Data> extractFeatures(final ScanRecord scanRecord) {
        if (scanRecord == null) {
            return null;
        }
        // Get message data
        final List<Data> featureList = new ArrayList<>();
        final List<Data> messages = extractMessages(scanRecord.getBytes());
        if (messages != null) {
            featureList.addAll(messages);
        }
        return featureList;
    }

    /// Match filter patterns against data items, returning the first match
    protected final static MatchingPattern match(final List<FilterPattern> patternList, final Data rawData) {
        // No pattern to match against
        if (patternList == null || patternList.isEmpty()) {
            return null;
        }
        // Empty raw data
        if (rawData == null || rawData.value.length == 0) {
            return null;
        }
        // Extract messages
        final List<Data> messages = extractMessages(rawData.value);
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (Data message : messages) {
            final String hexEncodedString = message.hexEncodedString();
            final FilterPattern pattern = match(patternList, hexEncodedString);
            if (pattern != null) {
                return new MatchingPattern(pattern, hexEncodedString);
            }
        }
        return null;
    }

    /// Match scan record messages against all registered patterns, returns matching pattern or null.
    public MatchingPattern match(final BLEDevice device) {
        try {
            final ScanRecord scanRecord = device.scanRecord();
            // Cannot match device without any scan record data
            if (scanRecord == null) {
                return null;
            }
            final Data rawData = new Data(scanRecord.getBytes());
            return match(filterPatterns, rawData);
        } catch (Throwable e) {
            // Errors are expected to be common place due to corrupted or malformed advert data
            return null;
        }
    }
}
