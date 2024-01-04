public class Foo<T, U> {
	private T value1;

	private U value2;

	public void bar(T value1, U value2) {
		this.value1= value1; // First generic type parameter
		this.value2= value2; // Second generic type parameter
	}
}
