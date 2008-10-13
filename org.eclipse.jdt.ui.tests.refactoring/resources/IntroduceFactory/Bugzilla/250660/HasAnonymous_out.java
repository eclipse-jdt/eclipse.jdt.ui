package p;

import java.util.List;

public class HasAnonymous {

    public static HasAnonymous createHasAnonymous() {
		return new HasAnonymous();
	}

	protected /*[*/HasAnonymous/*]*/() {
    }

    public void foo() {
        new HasAnonymous() {
            public void foo() {
            }
        };
    }
}
