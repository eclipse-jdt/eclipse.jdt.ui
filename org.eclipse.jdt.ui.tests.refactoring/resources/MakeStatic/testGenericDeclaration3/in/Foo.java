public class Foo<T, U, Z> {
	private T value1;

	private U value2;

	public <T, U> void bar(Foo<T, U, Z> foo, T value1, U value2) {
		foo.value1= value1;
		foo.value2= value2;
	}
}
