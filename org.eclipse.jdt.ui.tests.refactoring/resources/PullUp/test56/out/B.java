package p;

class B extends A {
	private int i;
	protected void foo2() {
		this.m(this);
	}	
	void foo() {
		this.i = 4;
	}
}