public class Foo {

	String j= "";

	public String bar(String s) {
		String i= this.j;
		return i;
	}

	public static String bar(String s, Foo foo) {
		String i= foo.j;
		return i;
	}
}
