class Foo<A> {

	int j;

	/**
	 * Some documentation ...
	 * 
	 * @author someone
	 * @param foo
	 * @param <A>
	 * @see Object#equals(Object)
	 * @throws IllegalArgumentException
	 */
	private static <A> void bar(Foo<A> foo) throws IllegalArgumentException {
		foo.j= 0;
	}
}
