package p;

public class A {
	static class B {
		static int f = 22;
	}

	static int f = 42;

	static B foo() {
		B r = new B() {
			public int m() {
				for (A B = new A(); B.equals(null); B = null) {
				}
				return B.f;
			}
		};
		A B = new A();
		return r;
	}
}
