package p;
class A {
	public void m2() {
	}
}
class B extends A {
	public void test() {
		m(this);
	}

	public void m(A a) {
		a.m2();
	}
}