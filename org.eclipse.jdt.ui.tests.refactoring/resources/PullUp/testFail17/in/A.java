package p;
class A{
	public void m() {
	}
}
class C2 extends A{
}
class C1 extends C2{
}
class B extends C1 {
	public void m() {
	}
}
class B1 extends C1{
	public void foo() {
		m();//if you move B.m() to C2 this will result in a different call
	}
}

