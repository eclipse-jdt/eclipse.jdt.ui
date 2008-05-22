package p;

public class A {
	private static final class B {
		private Class baz() {
			return getClass();
		}
	}

	private void bar() {
		new B();
	}
}