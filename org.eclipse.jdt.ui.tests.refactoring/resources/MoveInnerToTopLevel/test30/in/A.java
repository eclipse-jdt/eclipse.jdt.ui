import static java.lang.Math.E;
class A extends B
{
	private int foo() {return 0;}
	public class Inner
	{
		Inner() {
			int f= foo();
			int g= bar();
			double e= E;
		}
	}
	
	public A()
	{
		super();
		new A.Inner();
	}
}
class B extends C {
}
class C {
	protected int bar() {return 0;}
}