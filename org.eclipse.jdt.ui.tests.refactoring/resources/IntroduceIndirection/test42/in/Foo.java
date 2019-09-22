package p;

public class Foo {

	public static class Bar {

		public static class Baz {

			private void foo() { // <- create indirection in Foo
			}
		}
	}
}
