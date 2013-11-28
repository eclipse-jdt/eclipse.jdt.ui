package p;

class Test<T extends M> {
	// Invoke "Introduce Indirection" on 'm2.m()'
    void h(M2 m2) {
    	m2.m();
    }

    void f(T t) {
        t.m();
    }
}

class M {
	void m() { }
}

class M2 extends M {
	void m() { }
}