package p;
class SuperA{
	public void m() {
	}
}
class A extends SuperA{

	public void m() {
	}
}
class B extends A{
}
class B1 extends A{
	public void foo(){
		A a= null;
		a.m();
	}
}