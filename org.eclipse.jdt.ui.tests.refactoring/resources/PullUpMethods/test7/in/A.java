package p;

class A {
	void a(A a){}
}

class B extends A {
	void m() { 
		a(this);
	}
}