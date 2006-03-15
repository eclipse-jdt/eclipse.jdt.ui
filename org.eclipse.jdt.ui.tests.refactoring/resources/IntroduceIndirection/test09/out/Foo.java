package p;

public class Foo {
	
	// Test warnings for incorrectly qualified static calls
	
	static void foo() {	// <- invoke here (change name!)
		
	}
	
	/**
	 * 
	 */
	public static void bar() {
		Foo.foo();
	}

	Foo getFoo() {
		return new Foo();
	}
	
	{
		Foo.bar();
		Foo.bar();
	}

}
