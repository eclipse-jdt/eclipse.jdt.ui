package p;

class A {
	void a(){}

	public void m() { 
		this.a();
		this.a();
	}
}

class B extends A {
}