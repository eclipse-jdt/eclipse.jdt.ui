package p1;

public class Foo {

	public static void bar(String s) {
	}

	public static void foo() {
		Foo instance= new Foo();
		String j= Foo.bar("bar");
	}
}
