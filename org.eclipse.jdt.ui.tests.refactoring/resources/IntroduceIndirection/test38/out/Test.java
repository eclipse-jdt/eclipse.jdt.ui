package p;

class Test {
	static class C {
		static void foo() {
			Object o= C.class;
		}

		/**
		 * 
		 */
		public static void foo2() {
			C.foo();
		}
	}

	void f() {
		C.foo2();
	}
}