package p;
class A{	
}
class B extends A{
	public static int foo(){return 1;}
	protected int m= foo();
}