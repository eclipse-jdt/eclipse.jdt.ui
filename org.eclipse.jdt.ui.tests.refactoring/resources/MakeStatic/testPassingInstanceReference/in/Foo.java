public class Foo {

	Foo foo;

	public void bar() {
		method(this);
		method(this.foo);
		method(foo);
	}

	public void method(Foo foo) {
	}
}
