class Foo<A, B extends Runnable> {

	int j;

	/**
	 * Some documentation ...
	 * 
	 * @author someone
	 * @param foo
	 * @param value1
	 * @return empty string
	 * @param value2
	 * @param value3
	 * @see Object#equals(Object)
	 * @param <T>
	 * @param <Z>
	 * @param <A>
	 * @param <B>
	 * @throws IllegalArgumentException
	 */
	private static <T, Z, A, B extends Runnable> String bar(Foo<A, B> foo, T value1, T value2, Z value3) throws IllegalArgumentException {
		foo.j= 0;
		return "";
	}
}
