//13, 14, 13, 19
package p;

import java.util.List;

class A {
    private static final List<?> FOO= foo();
	public static List<?> foo() {
        return null;
    }
    void take(List<?> a) {
    }
    void bar() {
        take(FOO);
    }
}
