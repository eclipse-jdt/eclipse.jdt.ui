package p;
class A{	
}
class B extends A{
	public int foo(){return 1;}
	protected static int m= new B().foo();
}