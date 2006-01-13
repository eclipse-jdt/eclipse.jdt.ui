package p;

public class Foo {
	
	/* (non-Javadoc)
	 * @see p.Foo#setE(java.lang.Object)
	 */
	public static <E> void bar(Foo foo, E e) {
		foo.setE(e);
	}

	<E> void setE(E e) {
	}
	
	{
		this.<String>setE("");	// <-- invoke here
	}

}
