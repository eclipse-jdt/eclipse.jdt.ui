package p;

public class Foo<E, F, G extends Comparable<E>> {

	/**
	 * @param <E>
	 * @param <F>
	 * @param <G>
	 * @param <H>
	 * @param foo
	 * @param h
	 * @return
	 */
	public static <E, F, G extends Comparable<E>, H> H bar(Foo<E, F, G> foo, H h) {
		return foo.getFoo(h);
	}

	<H> H getFoo(H h) {
		return null;
	}
	
	{
		Foo f= new Foo();
		Foo.bar(f, null); // <-- invoke here
	}
	
}
