package p;

public class Foo {
	
	// Very simple test
	
	/**
	 * @param foo
	 */
	public static void bar(Foo foo) {
		foo.foo();
	}

	void foo() { 	// <-- invoke here
		
	}
	
	{
		Foo.bar(this);
	}

}
