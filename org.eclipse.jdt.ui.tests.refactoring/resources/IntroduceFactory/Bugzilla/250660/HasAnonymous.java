package p;

import java.util.List;

public class HasAnonymous {

    public /*[*/HasAnonymous/*]*/() {
    }

    public void foo() {
        new HasAnonymous() {
            public void foo() {
            }
        };
    }
}
