//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package au.gov.health.covidsafe.sensor.datatype;

public class Tuple<A, B> {
    private final String labelA, labelB;
    public final A a;
    public final B b;

    public Tuple(A a, B b) {
        this("a", a, "b", b);
    }

    public Tuple(String labelA, A a, String labelB, B b) {
        this.labelA = labelA;
        this.labelB = labelB;
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return "(" + labelA + "=" + a + "," + labelB + "=" + b + ")";
    }
}
