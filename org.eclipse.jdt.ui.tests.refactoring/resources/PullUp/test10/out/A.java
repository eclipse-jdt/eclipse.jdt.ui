package p;

class A {
	protected int j;
	void a(){}
	protected void m() { 
		this.j++;
		this.j= 0;
	}
}

class B extends A {
}