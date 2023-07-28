public class Foo {

	public int bar(int i) {
		return i;
	}

	public static void foo() {
		Foo instance= new Foo();
		int i= instance.bar(0);
	}
}
