package p;

class A {
	void a(){}
}

class B extends A {
	void m() { 
		super.a();
		super.a();
	}
}