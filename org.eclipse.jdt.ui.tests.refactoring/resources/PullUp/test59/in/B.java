package p;

class B extends A {
	private int i;
	protected void m() {
		this.foo2();
		a(this);
	}
	protected void foo2() {
		this.m();
	}	
	void foo() {
		this.i = 4;
	}
}