package p;

class A {
	void a(A a){}
}

class B extends A {
	protected void m() { 
		a(this);
	}
}