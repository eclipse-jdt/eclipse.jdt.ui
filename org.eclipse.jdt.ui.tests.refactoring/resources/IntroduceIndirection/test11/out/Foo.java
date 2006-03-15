package p;

public class Foo {
	
	// Test name clash with existing arguments
	
	/**
	 * @param foo
	 * @param target
	 * @param target1
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
