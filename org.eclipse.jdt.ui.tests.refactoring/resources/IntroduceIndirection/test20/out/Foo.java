package p;

public class Foo<E, F, G extends Comparable<E>> {

	/* (non-Javadoc)
	 * @see p.Foo#getFoo(java.lang.Object)
	 */
	public static <E, F, G extends Comparable<E>, H> H bar(Foo<E, F, G> target, H h) {
		return target.getFoo(h);
	}

	<H> H getFoo(H h) {
		return null;
	}
	
	{
		Foo f= new Foo();
		Foo.bar(f, null); // <-- invoke here
	}
	
}
