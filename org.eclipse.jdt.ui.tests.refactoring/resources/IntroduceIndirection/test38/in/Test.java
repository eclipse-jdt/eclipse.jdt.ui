package p;

class Test {
	static class C {
		static void foo() {
			Object o= C.class;
		}
	}

	void f() {
		C.foo();
	}
}