package p;

class A1 {
	int f = 23;

	static class B {
		static int f = 42;
	}

	int foo() {
		int r = bar(B.f);
		A1 B = this;
		return r;
	}
	
	private int bar(int x) {
		return x;
	}
}