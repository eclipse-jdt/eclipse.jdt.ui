package p;

class A {
}

class B extends A {
	private class P{};
	void m() { 
		P p= new P();
	}	
}