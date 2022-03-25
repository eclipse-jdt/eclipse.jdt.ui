package p;

class A1 {
	int f = 23;

	static class B {
		static int f = 42;
	}

	int foo() {
		A1 B = this;
		return bar(A1.B.f, A1.B.f);
	}
	
	private int bar(int x, int y) {
		return x + y;
	}
}