package com.atlassian.mobilekit.module.core.utils;

import android.text.TextUtils;

/**
 * This class exists as a workaround for Unit Test issues where TextUtils cannot be mocked
 * Refer: http://tools.android.com/tech-docs/unit-testing-support
 */
public final class StringUtils {

    public static final String EOL = "\n";
    private static final String ELLIPSIS = "\u2026";
    private static final int ELLIPSIS_LEN = ELLIPSIS.length();

    private StringUtils() {
    }

    public static String ellipsize(String input, int maxLen) {
        return (TextUtils.isEmpty(input) || input.length() <= (maxLen + ELLIPSIS_LEN))
                ? input
                : input.substring(0, maxLen) + ELLIPSIS;
    }
}
