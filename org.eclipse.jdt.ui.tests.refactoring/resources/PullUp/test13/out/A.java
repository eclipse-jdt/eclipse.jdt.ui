package p;

class A {

	protected void m() { 
		new B();
	}
}

class B extends A {
}