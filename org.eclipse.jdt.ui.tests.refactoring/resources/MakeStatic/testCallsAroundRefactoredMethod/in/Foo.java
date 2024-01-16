public class Foo {

	public void callerAbove() {
		Foo instance= new Foo();
		instance.enclosedMethod();
	}

	public void enclosedMethod() {
	}

	public void callerBelow() {
		Foo instance= new Foo();
		instance.enclosedMethod();
	}

}