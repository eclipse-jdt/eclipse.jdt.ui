package p;

public class Foo {
	
	// Test warnings for incorrectly qualified static calls
	
	static void foo() {	// <- invoke here (change name!)
		
	}
	
	Foo getFoo() {
		return new Foo();
	}
	
	{
		foo();
		Foo.foo();
	}

}
