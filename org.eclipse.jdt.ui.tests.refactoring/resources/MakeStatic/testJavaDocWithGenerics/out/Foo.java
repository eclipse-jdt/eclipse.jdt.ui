class Foo<A, B extends Runnable> {

	int j;

	/**
	 * @param foo
	 * @param <T>
	 * @param <Z>
	 * @param value1
	 * @param value2
	 * @param value3
	 * @param <A>
	 * @param <B>
	 */
	private static <T, Z, A, B extends Runnable> void bar(Foo<A, B> foo, T value1, T value2, Z value3) {
		foo.j= 0;
	}
}
