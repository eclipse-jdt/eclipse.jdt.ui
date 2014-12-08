package p;

class Test {
    /**
	 * @param test
	 * @param c
	 */
	public static void foo(Test test, C c) {
		test.foo(c);
	}

	void foo(C c) {
    }

    private class C {
    }
}