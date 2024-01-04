public class Foo {

	String j= "";

	public static String bar(Foo foo2, String foo) {
		String i= foo2.j;
		return i;
	}

	public void method() {
		String j= Foo.bar(this, "bar");
	}
}
