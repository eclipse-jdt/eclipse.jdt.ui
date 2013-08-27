package p;
class A {
	B b;
	public void m2() {
	}
}
class B extends A {
	public void test() {
		b.m(this);
	}

	public void m(A a) {
		a.m2();
	}
}