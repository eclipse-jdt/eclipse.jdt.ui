class A extends B
{
	int foo() {return 0;}
	public A()
	{
		super();
		new Inner(this);
	}
}
class B extends C {
}
class C {
	protected int bar() {return 0;}
}