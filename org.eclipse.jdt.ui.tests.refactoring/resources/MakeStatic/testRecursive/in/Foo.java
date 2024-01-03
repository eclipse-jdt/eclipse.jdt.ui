public class Foo {

	public void bar(Foo foo) {
		foo.bar(this);
	}

	public void foo() {
		Foo instance= new Foo();
		instance.bar(this);
	}
}
