package p;

import p.Foo.Bar.Baz;

public class Foo {

	public static class Bar {

		public static class Baz {

			private void foo() { // <- create indirection in Foo
			}
		}
	}

	/**
	 * @param baz
	 */
	public static void foo(Baz baz) {
		baz.foo();
	}
}
