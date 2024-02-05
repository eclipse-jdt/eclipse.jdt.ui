public class Foo {

	String j= "";

	public String bar(String ending) {
		String i= this.j + ending;
		i= j + ending;
		return i;
	}

	public void method() {
		String j= this.bar("bar");
	}

	public static void staticMethod() {
		Foo instance= new Foo();
		String j= instance.bar("bar");
	}
}
