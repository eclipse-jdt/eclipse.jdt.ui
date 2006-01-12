package p0;

abstract class Foo extends VerySuperFoo {
	
	{
		Foo foo= new RealFoo();
		foo.foo();				// <-- invoke here		
	}
}
