package p;
class A{	
	void x(){}
}
class B extends A{
	void x(){}
	protected void m(){ x();}
}