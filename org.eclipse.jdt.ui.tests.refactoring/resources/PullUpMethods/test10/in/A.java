package p;

class A {
	protected int j;
	void a(){}
}

class B extends A {
	void m() { 
		super.j++;
		super.j= 0;
	}
}