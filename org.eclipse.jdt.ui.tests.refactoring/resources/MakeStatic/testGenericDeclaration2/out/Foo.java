public class Foo<T, U> {
	private T value1;

	private U value2;

	public static <T, U> void bar(Foo<T, U> foo, T value1, U value2) {
		foo.value1= value1; // First generic type parameter
		foo.value2= value2; // Second generic type parameter
	}
}
