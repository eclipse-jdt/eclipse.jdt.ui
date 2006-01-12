package p;

public class Foo {
	
	// Test error, method already exists
	
	/* (non-Javadoc)
	 * @see p.Bar#foo(java.lang.String)
	 */
	public static void foo(Bar target, String foo) {
		target.foo(foo);
	}

	void foo(p.Bar bar, String foo)
	
	{
		Foo.foo(new Bar(), null); // <- invoke here with same name and target type Foo
	}

}
