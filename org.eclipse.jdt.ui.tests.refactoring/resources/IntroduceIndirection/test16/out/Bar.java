package p;

public class Bar {
	
	/* (non-Javadoc)
	 * @see p.Foo#foo()
	 */
	public static void bar(Foo foo) {
		foo.foo();
	}

	{
		Bar.bar(new Foo());
	}

}
