package p;

import java.util.ArrayList;

class A<T, U> {
	// cannot infer type for U -> leave raw
    T t;
    U u;
    
    void addT(T arg) {
        t= arg;
    }
    
    static void m() {
        A p = new A();
        p.addT("Hello");
    }
}
