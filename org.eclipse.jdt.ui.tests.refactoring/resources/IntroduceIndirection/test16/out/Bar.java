package p;

public class Bar {
	
	/* (non-Javadoc)
	 * @see p.Foo#foo()
	 */
	public static void bar(Foo target) {
		target.foo();
	}

	{
		Bar.bar(new Foo());
	}

}
