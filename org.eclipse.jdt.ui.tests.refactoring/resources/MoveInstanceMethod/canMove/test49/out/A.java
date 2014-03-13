package p;

class A {
	B b;
}

class B extends A {
	void test() {
		b.m(2);
	}

	protected void m(int i) {
	}
}