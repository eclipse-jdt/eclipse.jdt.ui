class A
{
	private int f= 0;
	public class Inner
	{
		Inner() {
			f= 1;
		}
	}
	
	public A()
	{
		super();
		new A.Inner();
	}
}
