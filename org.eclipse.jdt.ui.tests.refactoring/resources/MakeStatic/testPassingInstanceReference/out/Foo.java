public class Foo {

	Foo foo;

	public static void bar(Foo foo) {
		foo.method(foo);
		foo.method(foo.foo);
		foo.method(foo.foo);
	}

	public void method(Foo foo) {
	}
}
