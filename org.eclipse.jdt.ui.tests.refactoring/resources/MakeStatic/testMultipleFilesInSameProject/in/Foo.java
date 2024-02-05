package p1;

public class Foo {

	public void bar(String s) {
	}

	public static void foo() {
		Foo instance= new Foo();
		String j= instance.bar("bar");
	}
}
