package p0;

import p1.Bar;

public abstract class Foo extends VerySuperFoo {
	
	{
		Foo foo= new RealFoo();
		Bar.bar(foo);				// <-- invoke here		
	}
}
