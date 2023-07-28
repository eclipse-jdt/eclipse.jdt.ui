public class Foo<T> {
	private T value;

	public static <T> void bar(Foo<T> foo, T value) {
		foo.value= value;
	}
}
