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
		m();//if you move B.m() to A this will result in a different call
	}
}
