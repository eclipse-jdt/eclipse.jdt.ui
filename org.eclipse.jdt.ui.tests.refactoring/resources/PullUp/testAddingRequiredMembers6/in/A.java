package p;
class A{	
	void x(){}
}
class B extends A{
	public int hashCode(){return 1;}
	protected int m(){ return hashCode();}
}