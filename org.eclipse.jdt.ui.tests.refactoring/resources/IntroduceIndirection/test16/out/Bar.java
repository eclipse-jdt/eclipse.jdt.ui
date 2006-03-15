package p;

public class Bar {
	
	/**
	 * @param foo
	 */
	public static void bar(Foo foo) {
		foo.foo();
	}

	{
		Bar.bar(new Foo());
	}

}
