public class Foo {

	public static String bar(String ending) {
		String i= "bar" + ending;
		return i;
	}

	public static void foo() {
		Foo instance= new Foo();
		String j= Foo.bar("foo");
	}
}
