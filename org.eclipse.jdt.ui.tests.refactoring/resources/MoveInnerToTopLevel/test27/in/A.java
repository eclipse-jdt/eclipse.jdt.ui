class A
{
	private int foo() {return 0};
	public class Inner
	{
		Inner() {
			int f= foo();
		}
	}
	
	public A()
	{
		super();
		new A.Inner();
	}
}
