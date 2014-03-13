package p;

class Test<T extends Runnable & M> {
	/**
	 * @param m
	 */
	public static void foo(M m) {
		m.m();
	}

	// Invoke "Introduce Indirection" on 'm2.m()'
    void h(M2 m2) {
    	Test.foo(m2);
    }

    void f(T t) {
        Test.foo(t);
    }
}

interface M {
	void m();
}

interface M2 extends M {
	void m();
}