package p;

class A {
}

class B extends A {

	protected void m() {
	}
}

class C extends B {
	void test(B b) {
		b.m();
	}
}