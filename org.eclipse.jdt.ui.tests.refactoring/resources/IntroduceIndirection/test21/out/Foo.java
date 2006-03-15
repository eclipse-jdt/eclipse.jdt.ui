package p;

public class Foo {
	
	/**
	 * @param <E>
	 * @param foo
	 * @param e
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
