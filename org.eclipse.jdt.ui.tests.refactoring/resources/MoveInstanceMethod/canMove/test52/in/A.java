package p;

class A {
	B b;
	void m(int i) {
	}
}

class B extends A {
}

class C extends B {
	void test() {
		super.m(2);
	}
}