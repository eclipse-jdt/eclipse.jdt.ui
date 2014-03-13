package p;

class Test<T extends Runnable & M3> {
	/**
	 * @param m2
	 */
	public static void foo(M2 m2) {
		m2.m();
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

interface M3 extends M2 {
}