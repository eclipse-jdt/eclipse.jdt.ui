package p;

class SuperA{
	protected void x(){}
}
class A extends SuperA{

	public void m() { 
		super.x();
	}
}
class B extends A {
}