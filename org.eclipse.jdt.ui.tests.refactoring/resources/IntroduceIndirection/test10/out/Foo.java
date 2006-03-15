package p;

public class Foo {
	
	// Test error, method already exists
	
	/**
	 * @param bar
	 * @param foo
	 */
	public static void foo(Bar bar, String foo) {
		bar.foo(foo);
	}

	void foo(p.Bar bar, String foo)
	
	{
		Foo.foo(new Bar(), null); // <- invoke here with same name and target type Foo
	}

}
