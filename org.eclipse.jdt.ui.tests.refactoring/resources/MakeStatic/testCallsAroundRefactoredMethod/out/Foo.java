public class Foo {

	public void callerAbove() {
		Foo instance= new Foo();
		Foo.enclosedMethod();
	}

	public static void enclosedMethod() {
	}

	public void callerBelow() {
		Foo instance= new Foo();
		Foo.enclosedMethod();
	}

}