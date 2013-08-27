package p;

class A {
	B b;
	void m(int i) {
	}
}

class B extends A {
	void test() {
		super.m(2);
	}
}