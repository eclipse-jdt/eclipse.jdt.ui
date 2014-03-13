package p;
class A {
	public void m(B b) {
		m2();
	}
	
	public void m2() {
	}
}
class B extends A {
	public void test() {
		super.m(this);
	}
}