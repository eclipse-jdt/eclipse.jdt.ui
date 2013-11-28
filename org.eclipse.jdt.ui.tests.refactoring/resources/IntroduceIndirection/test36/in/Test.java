package p;

class Test<T extends Runnable & M3> {
	// Invoke "Introduce Indirection" on 'm2.m()'
    void h(M2 m2) {
    	m2.m();
    }

    void f(T t) {
        t.m();
    }
}

interface M {
	void m();
}

interface M2 extends M {
	void m();
}

interface M3 extends M2 {
}