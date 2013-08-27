package p;

class A {
	void m(B b) {
	}
}

class B extends A {
}

class C extends B {
	void test() {
		super.m(this);
	}
}