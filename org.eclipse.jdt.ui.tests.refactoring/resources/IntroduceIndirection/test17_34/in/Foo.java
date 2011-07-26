package p;

import java.lang.invoke.MethodHandle;

public class Foo {
    void m(MethodHandle mh) throws Throwable {
    	mh.invokeGeneric(1, "abc", null);
    }
}
