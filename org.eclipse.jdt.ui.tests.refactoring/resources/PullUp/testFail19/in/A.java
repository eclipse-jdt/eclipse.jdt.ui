package p;
class A{}
class C1 extends A{
	public int a= 0;
}
class B extends C1 {
	public void m() {
		super.a = 1;
	}
}

