class A
{
	int foo() {return 0};
	public A()
	{
		super();
		new Inner(this);
	}
}
