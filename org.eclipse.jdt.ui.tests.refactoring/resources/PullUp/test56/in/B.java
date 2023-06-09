package p;

class B extends A {
	private int i;
	protected void m() {
		a(this);
	}
	protected void foo2() {
		this.m();
	}	
	void foo() {
		this.i = 4;
	}
}