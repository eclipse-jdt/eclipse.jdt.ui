package p;
class A {
	B b;
	public void m() {
		m2();
	}
	
	public void m2() {
	}
}
class B extends A {
	public void test() {
		super.m();
	}
}