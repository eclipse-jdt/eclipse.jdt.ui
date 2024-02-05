public class Foo {

	String j= "";

	public static String bar(Foo foo, String s) {
		String i= foo.j;
		return i;
	}

	public static String bar(String s, Foo foo) {
		String i= foo.j;
		return i;
	}
}
