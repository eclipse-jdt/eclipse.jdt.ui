package p;
class A{
	A(int i){}
}
class B extends A{
	B(){
		super(3);
	}
	public void m() {
	}
	void f(){
		A a= new A(4);
	}
}