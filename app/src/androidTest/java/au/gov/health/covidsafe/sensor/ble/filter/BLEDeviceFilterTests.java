//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.ble.filter;

import au.gov.health.covidsafe.sensor.datatype.Data;

import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BLEDeviceFilterTests {

    @Test
    public void testHexTransform() throws Exception {
        final Random random = new Random(0);
        for (int i = 0; i < 1000; i++) {
            final byte[] expected = new byte[i];
            random.nextBytes(expected);
            final String hex = new Data(expected).hexEncodedString();
            final byte[] actual = Data.fromHexEncodedString(hex).value;
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testExtractMessages_iPhoneX_F() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011A020A0C0BFF4C001006071EA3DD89E014FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(2, messages.size());
        assertEquals("1006071EA3DD89E0", messages.get(0).hexEncodedString());
        assertEquals("0100000000000000000000200000000000", messages.get(1).hexEncodedString());

        assertNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....1E"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^0100"}), raw));
    }

    @Test
    public void testExtractMessages_iOS12() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011A14FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(1, messages.size());
        assertEquals("0100000000000000000000200000000000", messages.get(0).hexEncodedString());

        assertNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^0100"}), raw));
    }

    @Test
    public void testExtractMessages_iPhoneX_J() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011A020A0C0BFF4C0010060C1E4FDE4DF714FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(2, messages.size());
        assertEquals("10060C1E4FDE4DF7", messages.get(0).hexEncodedString());
        assertEquals("0100000000000000000000200000000000", messages.get(1).hexEncodedString());

        assertNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....1E", "^10....14"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^0100"}), raw));
    }

    @Test
    public void testExtractMessages_MacBookPro_F() throws Exception {
        final Data raw = Data.fromHexEncodedString("0201060AFF4C001005421C1E616A000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(1, messages.size());
        assertEquals("1005421C1E616A", messages.get(0).hexEncodedString());

        assertNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"}), raw));
        assertNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^0100"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....1C", "^10....14"}), raw));
    }

    @Test
    public void testExtractMessages_iPhoneSE1_with_Herald() throws Exception {
        // iPhoneSE 1st gen w/ Herald
        final Data raw = Data.fromHexEncodedString("02011a020a0c11079bfd5bd672451e80d3424647af328142");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertNull(messages);
    }

    @Test
    public void testExtractMessages_iPhoneSE1_Background() throws Exception {
        // iPhoneSE 1st gen background
        final Data raw = Data.fromHexEncodedString("02011a020a0c14ff4c000100000000000000000000200000000000");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(1, messages.size());
        assertEquals("0100000000000000000000200000000000", messages.get(0).hexEncodedString());

        assertNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^0100"}), raw));
    }

    @Test
    public void testExtractMessages_iPhoneX_A() throws Exception {
        // iPhoneX
        final Data raw = Data.fromHexEncodedString("1eff4c001219006d17255505df2aec6ef580be0ddeba8bb034c996de5b0200");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(1, messages.size());
        assertEquals("1219006d17255505df2aec6ef580be0ddeba8bb034c996de5b0200".toUpperCase(), messages.get(0).hexEncodedString());

        assertNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"}), raw));
        assertNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^0100"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14", "^12"}), raw));
    }

    @Test
    public void testExtractMessages_iPhone7_A() throws Exception {
        // iPhone7
        final Data raw = Data.fromHexEncodedString("0bff4c001006061a396363ce");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(1, messages.size());
        assertEquals("1006061a396363ce".toUpperCase(), messages.get(0).hexEncodedString());

        assertNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"}), raw));
        assertNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^0100"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14", "^10....1a"}), raw));
    }

    @Test
    public void testExtractMessages_iPhone_nRFApp() throws Exception {
        // nRFConnect app running on iPhone - a Valid device
        final Data raw = Data.fromHexEncodedString("1bff4c000c0e00c857ac085510515d52cf3862211006551eee51497a");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(2, messages.size()); // AFC This returns 1 if parsed incorrectly
        assertEquals("0c0e00c857ac085510515d52cf386221".toUpperCase(), messages.get(0).hexEncodedString()); // AFC too long - Current implementation incorrectly ignores the apple segment length, 0e
        assertEquals("1006551eee51497a".toUpperCase(), messages.get(1).hexEncodedString()); // AFC This is not returned as a separate segment

        assertNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"}), raw));
        assertNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^0100"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14", "^10....1E"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14", "^10....1e"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14", "^0C"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14", "^0c"}), raw));
    }

    @Test
    public void testExtractMessages_AppleTV() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a020a0c0aff4c00100508141bba69");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(1, messages.size());
        assertEquals("100508141BBA69", messages.get(0).hexEncodedString());

        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"}), raw));
    }

    @Test
    public void testExtractMessages_Coincidence() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a020a0c0aff4c0010050814ff4c00");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(1, messages.size());
        assertEquals("10050814FF4C00", messages.get(0).hexEncodedString());

        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"}), raw));
    }

    @Test
    public void testExtractMessages_MultipleAppleSegments() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a0dff4c0010050814123456100101");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(2, messages.size());
        assertEquals("10050814123456", messages.get(0).hexEncodedString());
        assertEquals("100101", messages.get(1).hexEncodedString());

        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10..01"}), raw));
    }

    @Test
    public void testExtractMessages_LegacyZeroOne() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a020a0c0aff4c001005031c8ba89d14ff4c000100200000000000000000000000000000000000000000000000000000000000000000000000000000");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(2, messages.size());
        assertEquals("1005031c8ba89d".toUpperCase(), messages.get(0).hexEncodedString());
        assertEquals("0100200000000000000000000000000000", messages.get(1).hexEncodedString());

        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....1C"}), raw));
        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^01[0-9A-F]{32}$"}), raw));
    }

    @Test
    public void testExtractMessages_MacbookPro_A() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a0aff4c001005031c0b4cac");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(1, messages.size());
        assertEquals("1005031C0B4CAC", messages.get(0).hexEncodedString());

        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....1C"}), raw));
    }

    @Test
    public void testExtractMessages_MacbookProUnderflow() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a0aff4c001005031c0b4c");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertNull(messages);
    }

    @Test
    public void testExtractMessages_MacbookProOverflow() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a0aff4c001005031c0b4cac02011a0aff4c00100503");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(1, messages.size());
        assertEquals("1005031C0B4CAC", messages.get(0).hexEncodedString());

        assertNotNull(BLEDeviceFilter.match(BLEDeviceFilter.compilePatterns(new String[]{"^10....1C"}), raw));
    }


    @Test
    public void testCompilePatterns() throws Exception {
        final List<BLEDeviceFilter.FilterPattern> filterPatterns = BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"});
        assertEquals(2, filterPatterns.size());
        assertNotNull(BLEDeviceFilter.match(filterPatterns, "10060C044FDE4DF7"));
        assertNotNull(BLEDeviceFilter.match(filterPatterns, "10060C144FDE4DF7"));

        // Ignoring dots
        assertNotNull(BLEDeviceFilter.match(filterPatterns, "10XXXX044FDE4DF7"));
        assertNotNull(BLEDeviceFilter.match(filterPatterns, "10XXXX144FDE4DF7"));

        // Not correct values
        assertNull(BLEDeviceFilter.match(filterPatterns, "10060C054FDE4DF7"));
        assertNull(BLEDeviceFilter.match(filterPatterns, "10060C154FDE4DF7"));

        // Not start of pattern
        assertNull(BLEDeviceFilter.match(filterPatterns, "010060C054FDE4DF7"));
        assertNull(BLEDeviceFilter.match(filterPatterns, "010060C154FDE4DF7"));
    }

    @Test
    public void testMatch_iPhoneX_F() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011A020A0C0BFF4C001006071EA3DD89E014FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000");
        final List<Data> messages = BLEDeviceFilter.extractMessages(raw.value);
        assertEquals(2, messages.size());
        assertEquals("1006071EA3DD89E0", messages.get(0).hexEncodedString());
        assertEquals("0100000000000000000000200000000000", messages.get(1).hexEncodedString());
    }
}