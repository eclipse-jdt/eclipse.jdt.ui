class A
{
	public static class Inner
	{	
	}
	
	public A()
	{
		super();
		new A.Inner();
	}

}