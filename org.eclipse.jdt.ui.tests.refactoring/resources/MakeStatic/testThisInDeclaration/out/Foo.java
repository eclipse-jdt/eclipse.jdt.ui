public class Foo {

	String j= "";

	public static String bar(Foo foo, String ending) {
		String i= foo.j + ending;
		i= foo.j + ending;
		return i;
	}

	public void method() {
		String j= Foo.bar(this, "bar");
	}

	public static void staticMethod() {
		Foo instance= new Foo();
		String j= Foo.bar(instance, "bar");
	}
}
