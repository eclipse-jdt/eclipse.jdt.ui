public class Foo {

	int j;

	public static void bar(Foo foo, String... items) {
		foo.j= 0;
	}

	public void baz() {
		Foo foo= new Foo();
		Foo.bar(foo, "A", "B", "C");
	}
}
