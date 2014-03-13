package p;

class A {
	void m(B b) {
	}
}

class B extends A {
	void test() {
		super.m(this);
	}
}