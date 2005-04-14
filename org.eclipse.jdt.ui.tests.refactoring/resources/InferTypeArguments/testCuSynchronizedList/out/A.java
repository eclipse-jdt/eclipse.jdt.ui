package p;

import java.util.*;

class A {
    private final List<?> _binPaths = Collections.synchronizedList(new ArrayList<Object>());
    
    List<?> get() {
        return new ArrayList<Object>(_binPaths);
    }
}