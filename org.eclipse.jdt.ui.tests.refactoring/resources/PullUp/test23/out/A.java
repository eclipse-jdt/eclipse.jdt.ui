package p;

class SuperA{
	void x(){}
}
class A extends SuperA{

	public void m() { 
		super.x();
	}
}
class B extends A {
}