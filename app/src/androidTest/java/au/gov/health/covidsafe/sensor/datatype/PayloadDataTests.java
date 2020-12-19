//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class PayloadDataTests {

    @Test
    public void testShortName() throws Exception {
        for (int i=0; i<600; i++) {
            final PayloadData payloadData = new PayloadData((byte) 0, i);
            assertNotNull(payloadData);
        }
    }

}
