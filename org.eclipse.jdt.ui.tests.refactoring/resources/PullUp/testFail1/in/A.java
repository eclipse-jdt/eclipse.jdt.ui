package p;

class A {
	void a(A a){}
	void a(B b){}
}

class B extends A {
	void m() { 
		a(this);
	}
}
