package p;

class A {
	void a(){}

	protected void m() { 
		this.a();
		this.a();
	}

}

class B extends A {
	}