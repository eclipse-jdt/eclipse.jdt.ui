package p;

public class Foo {
	
	// Very simple test
	
	/* (non-Javadoc)
	 * @see p.Foo#foo()
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
