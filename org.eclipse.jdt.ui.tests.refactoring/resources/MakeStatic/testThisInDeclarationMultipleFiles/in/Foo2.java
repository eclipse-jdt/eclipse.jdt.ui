package p2;

import p1.Foo;

public class Foo2 {

	public void method() {
		Foo instance= new Foo();
		String j= instance.bar("bar");
	}

	public static void staticMethod() {
		Foo instance= new Foo();
		String j= instance.bar("bar");
	}
}
