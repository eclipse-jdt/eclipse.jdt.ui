package p;

class A {
	B b;
}

class B extends A {

	protected void m(int i) {
	}
}

class C extends B {
	void test() {
		b.m(2);
	}
}