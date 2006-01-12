package p;

public class Foo {
	
	/* (non-Javadoc)
	 * @see p.Foo#setE(java.lang.Object)
	 */
	public static <E> void bar(Foo target, E e) {
		target.setE(e);
	}

	<E> void setE(E e) {
	}
	
	{
		this.<String>setE("");	// <-- invoke here
	}

}
