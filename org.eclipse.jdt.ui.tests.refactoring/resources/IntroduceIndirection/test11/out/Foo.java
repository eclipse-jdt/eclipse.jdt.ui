package p;

public class Foo {
	
	// Test name clash with existing arguments
	
	/* (non-Javadoc)
	 * @see p.Foo#foo(java.lang.String, java.lang.String)
	 */
	public static void bar(Foo foo, String target, String target1) {
		foo.foo(target, target1);
	}

	void foo(String target, String target1) {
		
	}
	
	{
		Foo.bar(this, null, null); // <- invoke here
	}

}
