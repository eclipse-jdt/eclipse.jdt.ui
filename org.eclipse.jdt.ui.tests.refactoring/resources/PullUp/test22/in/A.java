package p;
class SuperA{
	public void m() {
	}
}
class A extends SuperA{
}
class B extends A{
	public void m(){
	}
}
class B1 extends A{
	public void foo(){
		A a= null;
		a.m();
	}
}