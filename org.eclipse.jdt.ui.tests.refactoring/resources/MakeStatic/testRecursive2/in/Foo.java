public class Foo {
	Foo method() {
		return new Foo();
	}

	void bar() {
		method().bar();
	}
}
