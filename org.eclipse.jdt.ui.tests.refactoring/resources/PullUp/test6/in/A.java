package p;

class A {
	int x;
}

class B extends A {
	protected void m() { 
		super.x++;
	}
}
