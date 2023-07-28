public class Foo {

	public String bar(String ending) {
		String i= "bar" + ending;
		return i;
	}

	public static void foo() {
		Foo instance= new Foo();
		String j= instance.bar("foo");
	}
}
