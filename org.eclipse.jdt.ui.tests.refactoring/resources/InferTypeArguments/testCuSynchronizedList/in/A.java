package p;

import java.util.*;

class A {
    private final List _binPaths = Collections.synchronizedList(new ArrayList());
    
    List get() {
        return new ArrayList(_binPaths);
    }
}