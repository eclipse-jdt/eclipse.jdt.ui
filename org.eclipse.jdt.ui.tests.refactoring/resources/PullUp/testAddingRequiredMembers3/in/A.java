package p;
class A{	
	void x(){}
}
class B extends A{
	void y(){}
	protected void m(){ y();}
}