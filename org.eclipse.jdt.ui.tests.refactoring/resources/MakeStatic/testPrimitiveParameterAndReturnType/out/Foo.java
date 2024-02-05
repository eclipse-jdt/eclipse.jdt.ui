public class Foo {

	public static int bar(int i) {
		return i;
	}

	public static void foo() {
		Foo instance= new Foo();
		int i= Foo.bar(0);
	}
}
