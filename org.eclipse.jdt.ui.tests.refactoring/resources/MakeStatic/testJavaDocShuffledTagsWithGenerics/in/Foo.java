class Foo<A, B extends Runnable> {

	int j;

	/**
	 * Some documentation ...
	 * 
	 * @author someone
	 * @param value1
	 * @return empty string
	 * @param value2
	 * @param value3
	 * @see Object#equals(Object)
	 * @param <T>
	 * @param <Z>
	 * @throws IllegalArgumentException
	 */
	private <T, Z> String bar(T value1, T value2, Z value3) throws IllegalArgumentException {
		this.j= 0;
		return "";
	}
}
