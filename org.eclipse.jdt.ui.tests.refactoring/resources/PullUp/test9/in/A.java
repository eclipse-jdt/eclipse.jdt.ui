package p;

class A {
	void a(){}
}

class B extends A {
	public void m() { 
		super.a();
		super.a();
	}
}