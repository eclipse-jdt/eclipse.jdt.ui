package p;
class A{	
	void x(){}
}
class B extends A{
	void y(){m();}
	protected void m(){ y();}
}