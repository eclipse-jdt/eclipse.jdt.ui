package p;

public class A {
	static class B {
		static int f = 22;
	}

	static int f = 42;

	static B foo() {
		A B = new A();
		return new B() {
			public int m() {
				for (A B = new A(); B.equals(null); B = null) {
				}
				return A.B.f;
			}
		};
	}
}
