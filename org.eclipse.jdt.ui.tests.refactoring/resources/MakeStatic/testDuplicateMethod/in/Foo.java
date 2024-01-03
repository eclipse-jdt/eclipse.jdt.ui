public class Foo {

	String j = "";
	
	public String bar(String s) {
		String i = this.j;
		return i;
	}
	
	public static String bar(Foo foo, String s) {
		String i = foo.j;
		return i;
	}
}