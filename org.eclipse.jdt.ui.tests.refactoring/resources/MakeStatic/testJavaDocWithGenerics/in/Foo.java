class Foo<A, B extends Runnable> {

	int j;

	/**
	 * @param <T>
	 * @param <Z>
	 * @param value1
	 * @param value2
	 * @param value3
	 */
	private <T, Z> void bar(T value1, T value2, Z value3) {
		this.j= 0;
	}
}
